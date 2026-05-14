package global.exception;

import domain.auth.exception.EmailAlreadyRegisteredException;
import domain.auth.exception.EmailNotVerifiedException;
import domain.auth.exception.VerificationAttemptsExceededException;
import domain.auth.exception.VerificationCodeMismatchException;
import domain.auth.exception.VerificationLockedException;
import domain.auth.exception.VerificationNotFoundOrExpiredException;
import domain.auth.exception.VerificationSendCooldownException;
import domain.auth.exception.VerificationSendLimitExceededException;
import global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리: 도메인 예외와 Bean Validation 오류를 ApiResponse 형식으로 변환한다.
 * 실패 시: success, message, errorCode (루트).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(message, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailTaken(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(VerificationSendCooldownException.class)
    public ResponseEntity<ApiResponse<Void>> handleCooldown(VerificationSendCooldownException ex) {
        String message = ex.getMessage() + " (재시도 가능까지 약 " + ex.getRetryAfterSeconds() + "초)";
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.failure(message, ex.getErrorCode()));
    }

    @ExceptionHandler(VerificationSendLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleSendLimit(VerificationSendLimitExceededException ex) {
        String message = ex.getMessage() + " (윈도우 만료까지 약 " + ex.getRetryAfterSeconds() + "초)";
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.failure(message, ex.getErrorCode()));
    }

    @ExceptionHandler(VerificationNotFoundOrExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(VerificationNotFoundOrExpiredException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(VerificationLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(VerificationLockedException ex) {
        String message = ex.getMessage() + " (잠금 해제까지 약 " + ex.getRetryAfterSeconds() + "초)";
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.failure(message, ex.getErrorCode()));
    }

    @ExceptionHandler(VerificationCodeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMismatch(VerificationCodeMismatchException ex) {
        String message = ex.getMessage() + " (남은 시도: " + ex.getRemainingAttempts() + "회)";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(message, ex.getErrorCode()));
    }

    @ExceptionHandler(VerificationAttemptsExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleAttempts(VerificationAttemptsExceededException ex) {
        String message = ex.getMessage() + " (잠금 해제까지 약 " + ex.getRetryAfterSeconds() + "초)";
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.failure(message, ex.getErrorCode()));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex) {
        ex.printStackTrace();
        log.error("Unhandled exception (stack trace also printed to stderr)", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
    }
}
