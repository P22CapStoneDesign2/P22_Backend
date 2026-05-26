/**
    @Valid 실패 시 모든 필드 에러 메시지를 , 로 조합해서 반환
    (예: "이메일 형식이 올바르지 않습니다., 비밀번호는 8자 이상이어야 합니다.")
    Exception.class 핸들러를 최하단에 배치 → catch-all fallback
    log.warn vs log.error 구분: 비즈니스 예외는 warn, 미처리 예외는 error
 */

package com.capstone.eqh.global.exception;

import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.global.common.ApiResponse;
import com.capstone.eqh.global.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * @PreAuthorize 등 메서드 보안에서 던지는 인가 거부 처리.
     * 필터 체인 레벨 거부는 SecurityConfig.accessDeniedHandler 가 처리하므로,
     * 여기서는 컨트롤러 진입 후 메서드 보안에서 발생한 케이스만 다룬다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = isUnapprovedProf() ? ErrorCode.PROF_NOT_APPROVED : ErrorCode.FORBIDDEN;
        log.warn("[AccessDeniedException] {} - {}", errorCode, e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.failure(errorCode.getStatusCode(), errorCode.getMessage()));
    }

    private boolean isUnapprovedProf() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) {
            return false;
        }
        return cud.getUser().getRole() == Role.PROF && !cud.isActive();
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

    /**
     * 요청 본문 누락 또는 JSON 파싱 실패 처리.
     * - Content-Type 누락/오류, body 비어있음, JSON syntax 오류 등이 이 경로로 들어온다.
     * - catch-all 보다 위에 위치해 500 으로 새지 않도록 명시적으로 400 으로 변환한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadableException] {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.failure(errorCode.getStatusCode(), errorCode.getMessage()));
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