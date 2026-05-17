package com.capstone.eqh.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email-verification")
public class EmailVerificationProperties {

    private int codeTtlSeconds = 300;
    private int resendCooldownSeconds = 60;
    private int maxSendsPerHour = 5;
    private int sendWindowSeconds = 3600;
    private int maxVerifyAttempts = 5;
    private int lockDurationSeconds = 900;
    private int verifiedTtlSeconds = 1800;
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
