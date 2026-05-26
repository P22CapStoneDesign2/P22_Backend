package com.capstone.eqh.domain.user.password.exception;

/**
 * 비밀번호 재설정 메일 발송 실패 시 사용.
 */
public class PasswordResetMailException extends RuntimeException {

    public PasswordResetMailException(String message, Throwable cause) {
        super(message, cause);
    }
}
