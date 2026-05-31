package com.capstone.eqh.domain.user.password.exception;

import lombok.Getter;

/**
 * 비밀번호 재설정 API 전용 예외.
 * API 명세의 {@code success}/{@code message} 형식으로 응답하기 위해 사용한다.
 */
@Getter
public class PasswordResetException extends RuntimeException {

    private final boolean success;

    public PasswordResetException(boolean success, String message) {
        super(message);
        this.success = success;
    }

    public static PasswordResetException failure(String message) {
        return new PasswordResetException(false, message);
    }

    public static PasswordResetException success(String message) {
        return new PasswordResetException(true, message);
    }
}
