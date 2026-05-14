package domain.auth.redis;

import global.config.EmailVerificationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 이메일 인증 관련 Redis 접근을 한곳에 모은다.
 * 키 설계는 TTL 기반으로 자동 정리되도록 한다.
 *
 * <h2>Redis 키 규칙 (정규화된 이메일 소문자를 suffix로 사용)</h2>
 * <ul>
 *   <li>{@code ev:code:{email}} — 값 형식: {@code {failedAttempts}:{codeHash}} (HMAC-SHA256 hex), TTL = 코드 유효시간</li>
 *   <li>{@code ev:cooldown:{email}} — 재발송 쿨다운 플래그, 값 {@code 1}</li>
 *   <li>{@code ev:send:{email}} — 시간 윈도우 내 발송 횟수 카운터</li>
 *   <li>{@code ev:lock:{email}} — 검증 실패 한도 초과 잠금, 값 {@code 1}</li>
 *   <li>{@code ev:verified:{email}} — 이메일 인증 완료(회원가입 허용), 값 {@code 1}</li>
 * </ul>
 * <p>
 * 논리적 예시: {@code email:code:test@example.com} 대신 실제 키는 {@code ev:code:test@example.com} 이다.
 */
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

    /**
     * 검증 실패 누적 횟수와 코드 해시를 담는 Redis 값 포맷: "{failedAttempts}:{codeHash}".
     */
    public record CodeState(int failedAttempts, String codeHash) {
    }

    public boolean existsLock(String normalizedEmail) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX_LOCK + normalizedEmail));
    }

    /**
     * 재전송 쿨다운이 남아 있으면 남은 초를 반환하고, 없으면 empty.
     */
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

    /**
     * 시간 윈도우 내 발송 횟수를 증가시키고 현재 카운트를 반환한다.
     */
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

    private void setRawCode(String normalizedEmail, CodeState state, int ttlSeconds) {
        redis.opsForValue().set(PREFIX_CODE + normalizedEmail, serializeCodeState(state), Duration.ofSeconds(ttlSeconds));
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

    /**
     * 회원가입 완료 후 인증 완료 플래그를 제거해 재사용을 막는다.
     */
    public void deleteVerified(String normalizedEmail) {
        redis.delete(PREFIX_VERIFIED + normalizedEmail);
    }

    /**
     * 코드 값을 갱신하면서 기존 TTL을 최대한 유지한다(불일치 시도 기록).
     */
    public void updateCodeStateKeepTtl(String normalizedEmail, CodeState newState) {
        String key = PREFIX_CODE + normalizedEmail;
        long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttl <= 0) {
            return;
        }
        redis.opsForValue().set(key, serializeCodeState(newState), Duration.ofSeconds(ttl));
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
