package domain.auth.service;

import domain.auth.exception.EmailAlreadyRegisteredException;
import domain.auth.exception.EmailNotVerifiedException;
import domain.auth.exception.VerificationAttemptsExceededException;
import domain.auth.exception.VerificationCodeMismatchException;
import domain.auth.exception.VerificationLockedException;
import domain.auth.exception.VerificationNotFoundOrExpiredException;
import domain.auth.exception.VerificationSendCooldownException;
import domain.auth.exception.VerificationSendLimitExceededException;
import domain.auth.redis.EmailVerificationRedisStore;
import domain.auth.redis.EmailVerificationRedisStore.CodeState;
import domain.auth.repository.UserRepository;
import global.config.EmailVerificationProperties;
import global.response.ApiResponse;
import global.util.CryptoHashUtil;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 이메일 인증(발송/검증) 유스케이스를 담당한다.
 * Redis 저장소와 메일 발송, JPA 사용자 중복 검사를 조합한다.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationRedisStore redisStore;
    private final MailNotificationService mailNotificationService;
    private final EmailVerificationProperties properties;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationRedisStore redisStore,
            MailNotificationService mailNotificationService,
            EmailVerificationProperties properties
    ) {
        this.userRepository = userRepository;
        this.redisStore = redisStore;
        this.mailNotificationService = mailNotificationService;
        this.properties = properties;
    }

    /**
     * 인증번호를 생성·Redis에 저장하고 메일(또는 콘솔 목)로 발송한다.
     */
    public ApiResponse<Void> sendVerificationCode(String rawEmail) {
        System.out.println("[TRACE] 2. Service.sendVerificationCode 진입");
        log.info("[TRACE] 2. Service.sendVerificationCode 진입");

        String email = normalizeEmail(rawEmail);
        System.out.println("[TRACE] 2-1. 이메일 정규화 완료 email=" + email);
        log.info("[TRACE] 2-1. 이메일 정규화 완료 email={}", email);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(
                    "이미 가입된 이메일입니다.", "EMAIL_ALREADY_REGISTERED");
        }

        if (redisStore.existsLock(email)) {
            int retry = (int) redisStore.getLockTtlSeconds(email);
            throw new VerificationLockedException(
                    "인증 시도가 잠금 상태입니다. 잠시 후 다시 시도해 주세요.",
                    "VERIFICATION_LOCKED",
                    retry
            );
        }

        redisStore.getCooldownRemainingSeconds(email).ifPresent(remaining -> {
            throw new VerificationSendCooldownException(
                    "재전송은 " + remaining + "초 후에 가능합니다.",
                    "VERIFICATION_SEND_COOLDOWN",
                    remaining.intValue()
            );
        });

        long sendCount = redisStore.incrementSendCount(email);
        if (sendCount > properties.getMaxSendsPerHour()) {
            int retryAfter = (int) redisStore.getSendCountTtlSeconds(email);
            throw new VerificationSendLimitExceededException(
                    "인증번호 발송 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.",
                    "VERIFICATION_SEND_LIMIT_EXCEEDED",
                    retryAfter
            );
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String hash = hashCode(email, code);
        redisStore.saveCodeInitial(email, hash);
        redisStore.setCooldown(email, properties.getResendCooldownSeconds());

        System.out.println("[TRACE] Redis 저장 완료 (ev:code)");
        log.info("[TRACE] Redis 코드 저장 완료 email={}", email);

        mailNotificationService.sendVerificationCode(email, code);

        System.out.println("[TRACE] 메일 발송 메서드 호출 완료");
        log.info("[TRACE] 메일/콘솔 발송 호출 완료");

        return ApiResponse.success("인증번호가 전송되었습니다.");
    }

    /**
     * 사용자가 입력한 코드를 검증하고, 성공 시 Redis에 인증 완료 상태를 기록한다.
     */
    public void verifyCode(String rawEmail, String rawCode) {
        System.out.println("[TRACE] 2. Service.verifyCode 진입");
        log.info("[TRACE] 2. Service.verifyCode 진입");

        String email = normalizeEmail(rawEmail);
        String code = rawCode.trim();
        System.out.println("[TRACE] 2-1. 정규화 email=" + email);
        log.info("[TRACE] 2-1. 정규화 완료 email={}", email);

        if (redisStore.existsLock(email)) {
            int retry = (int) redisStore.getLockTtlSeconds(email);
            System.out.println("[TRACE] 실패: lock 존재 (ev:lock), retryAfterSec=" + retry);
            log.warn("[TRACE] 실패: lock 존재 email={} retryAfterSec={}", email, retry);
            throw new VerificationLockedException(
                    "인증 시도가 잠금 상태입니다. 잠시 후 다시 시도해 주세요.",
                    "VERIFICATION_LOCKED",
                    retry
            );
        }

        System.out.println("[TRACE] 3. Redis 조회 시작 (ev:code)");
        log.info("[TRACE] 3. Redis 코드 조회 email={}", email);

        CodeState state = redisStore.findCodeState(email)
                .orElseThrow(() -> {
                    System.out.println("[TRACE] 실패: 코드 없음 또는 TTL 만료");
                    log.warn("[TRACE] 실패: 코드 없음/만료 email={}", email);
                    return new VerificationNotFoundOrExpiredException(
                            "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해 주세요.",
                            "VERIFICATION_NOT_FOUND_OR_EXPIRED"
                    );
                });

        System.out.println("[TRACE] 4. Hash 비교 시작 (failedAttempts=" + state.failedAttempts() + ")");
        log.info("[TRACE] 4. Hash 비교 시작 email={} failedAttempts={}", email, state.failedAttempts());

        String expectedHash = state.codeHash();
        String actualHash = hashCode(email, code);

        if (!expectedHash.equals(actualHash)) {
            System.out.println("[TRACE] 실패: Hash 불일치");
            log.warn("[TRACE] 실패: Hash 불일치 email={}", email);
            handleMismatch(email, state);
        }

        System.out.println("[TRACE] 5. 인증 성공");
        log.info("[TRACE] 5. 인증 성공 email={}", email);

        redisStore.deleteCode(email);
        redisStore.markVerified(email);

        System.out.println("[TRACE] 6. verified 저장 완료 (ev:verified), 코드 키 삭제됨");
        log.info("[TRACE] 6. verified 저장 완료 email={}", email);
    }

    private void handleMismatch(String email, CodeState state) {
        int newFails = state.failedAttempts() + 1;
        if (newFails >= properties.getMaxVerifyAttempts()) {
            redisStore.setLock(email, properties.getLockDurationSeconds());
            redisStore.deleteCode(email);
            int retry = properties.getLockDurationSeconds();
            System.out.println("[TRACE] 실패: 최대 시도 초과 → lock 설정, code 삭제");
            log.warn("[TRACE] 실패: 최대 시도 초과 email={} lockSec={}", email, retry);
            throw new VerificationAttemptsExceededException(
                    "인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.",
                    "VERIFICATION_ATTEMPTS_EXCEEDED",
                    retry
            );
        }

        CodeState updated = new CodeState(newFails, state.codeHash());
        redisStore.updateCodeStateKeepTtl(email, updated);

        int remaining = properties.getMaxVerifyAttempts() - newFails;
        System.out.println("[TRACE] 실패: 코드 불일치, failedAttempts 갱신 → " + newFails + ", 남은 시도=" + remaining);
        log.warn("[TRACE] 실패: 코드 불일치 email={} newFails={} remaining={}", email, newFails, remaining);

        throw new VerificationCodeMismatchException(
                "인증번호가 일치하지 않습니다.",
                "VERIFICATION_CODE_MISMATCH",
                remaining
        );
    }

    /**
     * 회원가입 서비스에서 호출해, 이메일이 인증 완료 상태인지 강제한다.
     */
    public void requireEmailVerifiedForSignup(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (!redisStore.existsVerified(email)) {
            throw new EmailNotVerifiedException(
                    "이메일 인증이 완료되지 않았습니다.",
                    "EMAIL_NOT_VERIFIED"
            );
        }
    }

    /**
     * 회원가입 성공 후 인증 완료 플래그를 제거한다(재가입/재인증 흐름을 위해).
     */
    public void consumeEmailVerification(String rawEmail) {
        redisStore.deleteVerified(normalizeEmail(rawEmail));
    }

    public boolean isEmailVerified(String rawEmail) {
        return redisStore.existsVerified(normalizeEmail(rawEmail));
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String hashCode(String normalizedEmail, String plainCode) {
        String payload = normalizedEmail + "|" + plainCode;
        return CryptoHashUtil.hmacSha256Hex(properties.getHmacSecret(), payload);
    }
}
