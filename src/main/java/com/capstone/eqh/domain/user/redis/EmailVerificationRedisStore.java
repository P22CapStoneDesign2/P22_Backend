package com.capstone.eqh.domain.user.redis;

import com.capstone.eqh.global.config.EmailVerificationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// 이메일 인증 관련 Redis 키 접근을 한 곳에 모은다. 키 스키마는 design-docs/email-verification.md 참고.
@Component
public class EmailVerificationRedisStore {

    private static final String PREFIX_CODE = "ev:code:";
    private static final String PREFIX_COOLDOWN = "ev:cooldown:";
    private static final String PREFIX_SEND = "ev:send:";
    private static final String PREFIX_LOCK = "ev:lock:";
    private static final String PREFIX_VERIFIED = "ev:verified:";

    private final StringRedisTemplate redis;
    private final EmailVerificationProperties properties;

    public EmailVerificationRedisStore(StringRedisTemplate redis, EmailVerificationProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public record CodeState(int failedAttempts, String codeHash) {
    }

    public boolean existsLock(String normalizedEmail) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX_LOCK + normalizedEmail));
    }

    public Optional<Long> getCooldownRemainingSeconds(String normalizedEmail) {
        Long ttl = redis.getExpire(PREFIX_COOLDOWN + normalizedEmail, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return Optional.empty();
        }
        return Optional.of(ttl);
    }

    public void setCooldown(String normalizedEmail, int cooldownSeconds) {
        redis.opsForValue().set(PREFIX_COOLDOWN + normalizedEmail, "1", Duration.ofSeconds(cooldownSeconds));
    }

    public long incrementSendCount(String normalizedEmail) {
        String key = PREFIX_SEND + normalizedEmail;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(properties.getSendWindowSeconds()));
        }
        return count == null ? 0L : count;
    }

    public long getSendCountTtlSeconds(String normalizedEmail) {
        Long ttl = redis.getExpire(PREFIX_SEND + normalizedEmail, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return properties.getResendCooldownSeconds();
        }
        return ttl;
    }

    public void saveCodeInitial(String normalizedEmail, String codeHash) {
        setRawCode(normalizedEmail, new CodeState(0, codeHash), properties.getCodeTtlSeconds());
    }

    public Optional<CodeState> findCodeState(String normalizedEmail) {
        String raw = redis.opsForValue().get(PREFIX_CODE + normalizedEmail);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(raw));
    }

    public void deleteCode(String normalizedEmail) {
        redis.delete(PREFIX_CODE + normalizedEmail);
    }

    public void setLock(String normalizedEmail, int lockSeconds) {
        redis.opsForValue().set(PREFIX_LOCK + normalizedEmail, "1", Duration.ofSeconds(lockSeconds));
    }

    public long getLockTtlSeconds(String normalizedEmail) {
        Long ttl = redis.getExpire(PREFIX_LOCK + normalizedEmail, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? properties.getLockDurationSeconds() : ttl;
    }

    public void markVerified(String normalizedEmail) {
        redis.opsForValue().set(
                PREFIX_VERIFIED + normalizedEmail,
                "1",
                Duration.ofSeconds(properties.getVerifiedTtlSeconds())
        );
    }

    public boolean existsVerified(String normalizedEmail) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX_VERIFIED + normalizedEmail));
    }

    public void deleteVerified(String normalizedEmail) {
        redis.delete(PREFIX_VERIFIED + normalizedEmail);
    }

    // 코드 값을 갱신하면서 기존 TTL을 유지한다(불일치 시도 기록용).
    public void updateCodeStateKeepTtl(String normalizedEmail, CodeState newState) {
        String key = PREFIX_CODE + normalizedEmail;
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return;
        }
        redis.opsForValue().set(key, serializeCodeState(newState), Duration.ofSeconds(ttl));
    }

    private void setRawCode(String normalizedEmail, CodeState state, int ttlSeconds) {
        redis.opsForValue().set(
                PREFIX_CODE + normalizedEmail,
                serializeCodeState(state),
                Duration.ofSeconds(ttlSeconds)
        );
    }

    private String serializeCodeState(CodeState state) {
        return state.failedAttempts() + ":" + state.codeHash();
    }

    private CodeState deserialize(String raw) {
        int idx = raw.indexOf(':');
        if (idx < 0) {
            throw new IllegalStateException("Malformed verification payload");
        }
        int fails = Integer.parseInt(raw.substring(0, idx));
        String hash = raw.substring(idx + 1);
        return new CodeState(fails, hash);
    }
}
