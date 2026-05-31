package com.capstone.eqh.domain.user.password.redis;

import com.capstone.eqh.global.config.PasswordResetProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 비밀번호 재설정 요청 쿨다운(Redis).
 * <p>
 * 키 스키마: {@code pr:cooldown:{normalizedEmail}}
 * <p>
 * <b>DB-only 대안</b>: 이 컴포넌트 대신
 * {@link com.capstone.eqh.domain.user.password.repository.PasswordResetTokenRepository#findTopByUser_IdOrderByCreatedAtDesc}
 * 로 마지막 발급 시각을 조회하고 {@code createdAt + cooldown} 과 비교한다.
 */
@Component
public class PasswordResetRedisStore {

    private static final String PREFIX_COOLDOWN = "pr:cooldown:";
    /** 토큰 → 이메일 (TTL = 토큰 만료와 동일) */
    private static final String PREFIX_TOKEN = "pr:token:";

    private final StringRedisTemplate redis;
    private final PasswordResetProperties properties;

    public PasswordResetRedisStore(StringRedisTemplate redis, PasswordResetProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** 쿨다운이 남아 있으면 남은 초를 반환 */
    public Optional<Long> getCooldownRemainingSeconds(String normalizedEmail) {
        Long ttl = redis.getExpire(PREFIX_COOLDOWN + normalizedEmail, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return Optional.empty();
        }
        return Optional.of(ttl);
    }

    /** 재요청 제한(기본 60초) 설정 */
    public void setCooldown(String normalizedEmail) {
        redis.opsForValue().set(
                PREFIX_COOLDOWN + normalizedEmail,
                "1",
                Duration.ofSeconds(properties.getResendCooldownSeconds())
        );
    }

    /**
     * 재설정 토큰과 이메일을 Redis에 저장 (TTL 필수).
     */
    public void saveToken(String token, String normalizedEmail) {
        redis.opsForValue().set(
                PREFIX_TOKEN + token,
                normalizedEmail,
                Duration.ofMinutes(properties.getTokenTtlMinutes())
        );
    }

    /** Redis에 토큰이 있으면 연결된 이메일 반환 (만료 시 empty) */
    public Optional<String> findEmailByToken(String token) {
        String email = redis.opsForValue().get(PREFIX_TOKEN + token);
        return Optional.ofNullable(email);
    }

    /** 비밀번호 변경 완료 후 1회용 토큰 삭제 */
    public void deleteToken(String token) {
        redis.delete(PREFIX_TOKEN + token);
    }
}
