package com.capstone.eqh.domain.user.password.dto.response;

/**
 * 비밀번호 재설정 API 공통 응답 ({@code success}, {@code message}).
 */
public record PasswordResetResponse(
        boolean success,
        String message
) {
    public static PasswordResetResponse ok(String message) {
        return new PasswordResetResponse(true, message);
    }

    public static PasswordResetResponse fail(String message) {
        return new PasswordResetResponse(false, message);
    }
}
