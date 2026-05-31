package com.capstone.eqh.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 비밀번호 재설정 관련 설정.
 * <p>
 * Redis 미사용 환경에서는 {@code resend-cooldown-seconds} 만 DB(last token created_at)로
 * 대체할 수 있으며, {@link com.capstone.eqh.domain.user.password.service.PasswordResetService}
 * 주석의 DB-only 대안을 참고한다.
 */
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    /** 토큰 유효 시간(분). 기본 30분 */
    private int tokenTtlMinutes = 30;

    /** 동일 이메일 재요청 쿨다운(초). 기본 60초(1분) */
    private int resendCooldownSeconds = 60;

    /**
     * 프론트 비밀번호 재설정 페이지 베이스 URL.
     * 실제 링크: {frontendResetUrl}?token={token}
     */
    private String frontendResetUrl = "http://localhost:5174/reset-password";

    /** 발신자 표시용 (선택) */
    private String mailFrom;

    public int getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }

    public void setTokenTtlMinutes(int tokenTtlMinutes) {
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public String getFrontendResetUrl() {
        return frontendResetUrl;
    }

    public void setFrontendResetUrl(String frontendResetUrl) {
        this.frontendResetUrl = frontendResetUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }
}
