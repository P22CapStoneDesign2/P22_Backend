package com.capstone.eqh.domain.material.exception;

import com.capstone.eqh.domain.material.common.MaterialApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.capstone.eqh.domain.material.controller")
public class MaterialExceptionHandler {

    @ExceptionHandler(MaterialAccessDeniedException.class)
    public ResponseEntity<MaterialApiResponse<Void>> handleAccessDenied(MaterialAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MaterialApiResponse.fail(
                        MaterialAccessDeniedException.ERROR_CODE,
                        MaterialAccessDeniedException.DEFAULT_MESSAGE));
    }

    @ExceptionHandler(MaterialNotFoundException.class)
    public ResponseEntity<MaterialApiResponse<Void>> handleNotFound(MaterialNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MaterialApiResponse.fail(
                        MaterialNotFoundException.ERROR_CODE,
                        e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MaterialApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(MaterialApiResponse.fail("INVALID_INPUT", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MaterialApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "입력값이 올바르지 않습니다.")
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest()
                .body(MaterialApiResponse.fail("INVALID_INPUT", message));
    }
}
