package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.redis.EmailVerificationRedisStore;
import com.capstone.eqh.domain.user.redis.EmailVerificationRedisStore.CodeState;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.config.EmailVerificationProperties;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import com.capstone.eqh.global.util.CryptoHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationRedisStore redisStore;
    private final MailNotificationService mailNotificationService;
    private final EmailVerificationProperties properties;

    public void sendVerificationCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);

        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (redisStore.existsLock(email)) {
            throw new CustomException(ErrorCode.VERIFICATION_LOCKED);
        }

        redisStore.getCooldownRemainingSeconds(email).ifPresent(remaining -> {
            throw new CustomException(ErrorCode.VERIFICATION_SEND_COOLDOWN);
        });

        long sendCount = redisStore.incrementSendCount(email);
        if (sendCount > properties.getMaxSendsPerHour()) {
            throw new CustomException(ErrorCode.VERIFICATION_SEND_LIMIT_EXCEEDED);
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String hash = hashCode(email, code);
        redisStore.saveCodeInitial(email, hash);
        redisStore.setCooldown(email, properties.getResendCooldownSeconds());

        mailNotificationService.sendVerificationCode(email, code);
        log.debug("인증번호 발송 처리 완료 email={}", email);
    }

    public void verifyCode(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String code = rawCode.trim();

        if (redisStore.existsLock(email)) {
            throw new CustomException(ErrorCode.VERIFICATION_LOCKED);
        }

        CodeState state = redisStore.findCodeState(email)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND_OR_EXPIRED));

        String expectedHash = state.codeHash();
        String actualHash = hashCode(email, code);

        if (!expectedHash.equals(actualHash)) {
            handleMismatch(email, state);
        }

        redisStore.deleteCode(email);
        redisStore.markVerified(email);
        log.debug("이메일 인증 성공 email={}", email);
    }

    public void requireEmailVerifiedForSignup(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (!redisStore.existsVerified(email)) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    public void consumeEmailVerification(String rawEmail) {
        redisStore.deleteVerified(normalizeEmail(rawEmail));
    }

    private void handleMismatch(String email, CodeState state) {
        int newFails = state.failedAttempts() + 1;
        if (newFails >= properties.getMaxVerifyAttempts()) {
            redisStore.setLock(email, properties.getLockDurationSeconds());
            redisStore.deleteCode(email);
            throw new CustomException(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
        }

        redisStore.updateCodeStateKeepTtl(email, new CodeState(newFails, state.codeHash()));
        throw new CustomException(ErrorCode.VERIFICATION_CODE_MISMATCH);
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String hashCode(String normalizedEmail, String plainCode) {
        return CryptoHashUtil.hmacSha256Hex(properties.getHmacSecret(), normalizedEmail + "|" + plainCode);
    }
}
