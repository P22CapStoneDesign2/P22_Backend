package com.capstone.eqh.domain.user.password.service;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.password.dto.request.PasswordResetConfirmRequestDto;
import com.capstone.eqh.domain.user.password.dto.response.PasswordResetResponse;
import com.capstone.eqh.domain.user.password.entity.PasswordResetToken;
import com.capstone.eqh.domain.user.password.exception.PasswordResetException;
import com.capstone.eqh.domain.user.password.exception.PasswordResetMailException;
import com.capstone.eqh.domain.user.password.redis.PasswordResetRedisStore;
import com.capstone.eqh.domain.user.password.repository.PasswordResetTokenRepository;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.config.PasswordResetProperties;
import com.capstone.eqh.global.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 비밀번호 재설정 비즈니스 로직.
 * <p>
 * <b>Redis 쿨다운</b>: {@link PasswordResetRedisStore} 로 동일 이메일 1분 재요청 제한.<br>
 * <b>DB-only 대안</b>: Redis Store 대신
 * {@link PasswordResetTokenRepository#findTopByUser_IdOrderByCreatedAtDesc} 후
 * {@code lastCreatedAt.plusSeconds(cooldown).isAfter(now)} 이면 쿨다운 예외 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PasswordPolicy.REGEX);

    private static final String MSG_MAIL_SENT = "비밀번호 재설정 메일이 발송되었습니다.";
    private static final String MSG_TOKEN_VALID = "유효한 토큰입니다.";
    private static final String MSG_PASSWORD_CHANGED = "비밀번호가 성공적으로 변경되었습니다.";
    private static final String MSG_TOKEN_EXPIRED_VERIFY = "만료된 토큰입니다.";
    private static final String MSG_TOKEN_INVALID = "유효하지 않은 토큰입니다.";
    private static final String MSG_TOKEN_EXPIRED_RESET = "토큰이 만료되었습니다.";
    private static final String MSG_PASSWORD_MISMATCH = "비밀번호가 일치하지 않습니다.";
    private static final String MSG_COOLDOWN = "비밀번호 재설정 메일은 1분 후에 다시 요청할 수 있습니다.";
    private static final String MSG_EMAIL_NOT_REGISTERED = "가입되지 않은 이메일입니다.";
    private static final String MSG_SOCIAL_ACCOUNT = "소셜 로그인으로 가입된 계정입니다. 소셜 로그인을 이용해 주세요.";
    private static final String MSG_EMAIL_ELIGIBLE = "비밀번호 재설정이 가능한 이메일입니다.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetRedisStore redisStore;
    private final PasswordResetEmailService emailService;
    private final PasswordResetProperties properties;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호 재설정 화면 — 이메일 입력 시 가입·재설정 가능 여부 검증.
     */
    @Transactional(readOnly = true)
    public PasswordResetResponse validateEmailEligibility(String rawEmail) {
        assertEligibleForPasswordReset(normalizeEmail(rawEmail));
        return PasswordResetResponse.ok(MSG_EMAIL_ELIGIBLE);
    }

    /**
     * 비밀번호 재설정 메일 요청 (가입된 LOCAL 계정만 발송).
     */
    @Transactional
    public PasswordResetResponse requestReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = assertEligibleForPasswordReset(email);
        enforceCooldown(email);
        LocalDateTime now = LocalDateTime.now();

        tokenRepository.expireActiveTokensByUserId(user.getId(), now);

        String plainToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiredAt = now.plusMinutes(properties.getTokenTtlMinutes());
        tokenRepository.save(new PasswordResetToken(user, plainToken, expiredAt));
        redisStore.saveToken(plainToken, email);

        try {
            log.info("📨 메일 발송 시작: {}", email);
            emailService.sendPasswordResetLink(email, plainToken);
            log.info("📨 메일 발송 완료: {}", email);
        } catch (PasswordResetMailException e) {
            throw e;
        }

        redisStore.setCooldown(email);
        log.debug("비밀번호 재설정 토큰 발급 userId={}", user.getId());
        return PasswordResetResponse.ok(MSG_MAIL_SENT);
    }

    /**
     * 재설정 토큰 검증 (이메일 링크 진입 시).
     */
    @Transactional(readOnly = true)
    public PasswordResetResponse verifyToken(String rawToken) {
        resolveActiveToken(rawToken, true);
        return PasswordResetResponse.ok(MSG_TOKEN_VALID);
    }

    /**
     * 새 비밀번호 저장.
     */
    @Transactional
    public PasswordResetResponse resetPassword(PasswordResetConfirmRequestDto request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw PasswordResetException.failure(MSG_PASSWORD_MISMATCH);
        }
        if (!PASSWORD_PATTERN.matcher(request.newPassword()).matches()) {
            throw PasswordResetException.failure(PasswordPolicy.MESSAGE);
        }

        PasswordResetToken token = resolveActiveToken(request.token(), false);
        LocalDateTime now = LocalDateTime.now();

        User user = token.getUser();
        if (!isLocalPasswordAccount(user)) {
            throw PasswordResetException.failure(MSG_TOKEN_INVALID);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        token.markUsed();
        redisStore.deleteToken(normalizeToken(request.token()));
        tokenRepository.expireActiveTokensByUserId(user.getId(), now);

        log.info("비밀번호 재설정 완료 userId={}", user.getId());
        return PasswordResetResponse.ok(MSG_PASSWORD_CHANGED);
    }

    /**
     * 활성 토큰 조회 — DB used/expired 검증, Redis에 키가 있으면 이메일 일치 추가 확인.
     *
     * @param forVerify true면 만료 메시지를 검증 API 명세 문구로 반환
     */
    private PasswordResetToken resolveActiveToken(String rawToken, boolean forVerify) {
        String tokenValue = normalizeToken(rawToken);
        if (tokenValue.isEmpty()) {
            throw PasswordResetException.failure(MSG_TOKEN_INVALID);
        }

        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> PasswordResetException.failure(MSG_TOKEN_INVALID));

        LocalDateTime now = LocalDateTime.now();
        if (token.isUsed()) {
            throw PasswordResetException.failure(MSG_TOKEN_INVALID);
        }
        if (token.isExpired(now)) {
            throw PasswordResetException.failure(
                    forVerify ? MSG_TOKEN_EXPIRED_VERIFY : MSG_TOKEN_EXPIRED_RESET);
        }

        redisStore.findEmailByToken(tokenValue).ifPresent(redisEmail -> {
            String dbEmail = normalizeEmail(token.getUser().getEmail());
            if (!dbEmail.equals(redisEmail)) {
                throw PasswordResetException.failure(MSG_TOKEN_INVALID);
            }
        });

        return token;
    }

    private String normalizeToken(String rawToken) {
        return rawToken == null ? "" : rawToken.trim();
    }

    private void enforceCooldown(String normalizedEmail) {
        redisStore.getCooldownRemainingSeconds(normalizedEmail).ifPresent(remaining -> {
            throw PasswordResetException.failure(MSG_COOLDOWN);
        });
    }

    private boolean isLocalPasswordAccount(User user) {
        return !user.isDeleted()
                && user.getProvider() == AuthProvider.LOCAL
                && user.getPassword() != null;
    }

    /**
     * 가입 여부·LOCAL 계정·비밀번호 보유 여부를 검증한다.
     *
     * @return 재설정 가능한 사용자
     * @throws PasswordResetException 가입되지 않았거나 소셜 계정인 경우
     */
    private User assertEligibleForPasswordReset(String normalizedEmail) {
        User user = userRepository.findByEmail(normalizedEmail)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> PasswordResetException.failure(MSG_EMAIL_NOT_REGISTERED));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw PasswordResetException.failure(MSG_SOCIAL_ACCOUNT);
        }
        if (user.getPassword() == null) {
            throw PasswordResetException.failure(MSG_EMAIL_NOT_REGISTERED);
        }
        return user;
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }
}
