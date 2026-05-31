package com.capstone.eqh.domain.user.password.exception;

import com.capstone.eqh.domain.user.password.controller.PasswordResetController;
import com.capstone.eqh.domain.user.password.dto.response.PasswordResetResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 비밀번호 재설정 API 전용 예외 처리.
 * 명세의 {@code success}/{@code message} 응답 형식을 유지한다.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = PasswordResetController.class)
public class PasswordResetExceptionHandler {

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<PasswordResetResponse> handlePasswordResetException(PasswordResetException e) {
        log.warn("[PasswordReset] {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new PasswordResetResponse(false, e.getMessage()));
    }

    @ExceptionHandler(PasswordResetMailException.class)
    public ResponseEntity<PasswordResetResponse> handleMailException(PasswordResetMailException e) {
        log.error("[PasswordReset] 메일 발송 실패", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PasswordResetResponse.fail(
                        "비밀번호 재설정 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."));
    }

    /**
     * @Valid 실패 — 이메일 형식 등 명세 메시지 우선 반환.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PasswordResetResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값이 올바르지 않습니다.");

        boolean emailFormatError = e.getBindingResult().getFieldErrors().stream()
                .anyMatch(err -> "email".equals(err.getField())
                        && err.getDefaultMessage() != null
                        && err.getDefaultMessage().contains("이메일"));

        if (emailFormatError) {
            message = "올바른 이메일 형식이 아닙니다.";
        }

        log.warn("[PasswordReset] validation: {}", message);
        return ResponseEntity.badRequest().body(PasswordResetResponse.fail(message));
    }
}
