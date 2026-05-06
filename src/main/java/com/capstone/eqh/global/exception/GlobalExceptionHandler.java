/**
    @Valid 실패 시 모든 필드 에러 메시지를 , 로 조합해서 반환
    (예: "이메일 형식이 올바르지 않습니다., 비밀번호는 8자 이상이어야 합니다.")
    Exception.class 핸들러를 최하단에 배치 → catch-all fallback
    log.warn vs log.error 구분: 비즈니스 예외는 warn, 미처리 예외는 error
 */

package com.capstone.eqh.global.exception;

import com.capstone.eqh.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Slf4j ; 실제 컴파일 시점에 생성되는 코드
// private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
// valid에서 {}를 통한 성능 이점
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 예외 처리 */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("[CustomException] {} - {}", e.getErrorCode(), e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.failure(errorCode.getStatusCode(), errorCode.getMessage()));
    }

    /** @Valid / @Validated 유효성 검증 실패 처리 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[ValidationException] {}", message);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(400, message));
    }

    /** 예상치 못한 예외 처리 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.failure(errorCode.getStatusCode(), errorCode.getMessage()));
    }
}