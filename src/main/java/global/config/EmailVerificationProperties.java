package global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이메일 인증 정책 값을 application.yml에서 주입받는다.
 * 운영/개발 환경별로 TTL·한도를 조정할 수 있다.
 */
@ConfigurationProperties(prefix = "app.email-verification")
public class EmailVerificationProperties {

    /** 인증번호 TTL (초). 기본 5분. */
    private int codeTtlSeconds = 300;

    /** 동일 이메일 재발송 최소 간격 (초). */
    private int resendCooldownSeconds = 60;

    /** 1시간 내 최대 발송 횟수. */
    private int maxSendsPerHour = 5;

    /** 발송 카운트 윈도우 길이(초). 기본 1시간. */
    private int sendWindowSeconds = 3600;

    /** 인증번호 불일치 허용 횟수. */
    private int maxVerifyAttempts = 5;

    /** 검증 실패 초과 시 잠금 시간 (초). */
    private int lockDurationSeconds = 900;

    /** 인증 완료 후 회원가입 허용 윈도우 (초). */
    private int verifiedTtlSeconds = 1800;

    /** HMAC에 사용할 서버 비밀(반드시 환경변수로 덮어쓰기 권장). */
    private String hmacSecret = "change-me-in-production";

    public int getCodeTtlSeconds() {
        return codeTtlSeconds;
    }

    public void setCodeTtlSeconds(int codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int getMaxSendsPerHour() {
        return maxSendsPerHour;
    }

    public void setMaxSendsPerHour(int maxSendsPerHour) {
        this.maxSendsPerHour = maxSendsPerHour;
    }

    public int getSendWindowSeconds() {
        return sendWindowSeconds;
    }

    public void setSendWindowSeconds(int sendWindowSeconds) {
        this.sendWindowSeconds = sendWindowSeconds;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }

    public int getLockDurationSeconds() {
        return lockDurationSeconds;
    }

    public void setLockDurationSeconds(int lockDurationSeconds) {
        this.lockDurationSeconds = lockDurationSeconds;
    }

    public int getVerifiedTtlSeconds() {
        return verifiedTtlSeconds;
    }

    public void setVerifiedTtlSeconds(int verifiedTtlSeconds) {
        this.verifiedTtlSeconds = verifiedTtlSeconds;
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }
}
