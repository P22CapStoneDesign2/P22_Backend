package com.capstone.eqh.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 400 Bad Request ───────────────────────────────────────────────
    OAUTH2_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "소셜 계정에서 이메일 정보를 가져올 수 없습니다."),
    OAUTH2_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
    SOCIAL_CANNOT_CHANGE_PASSWORD(HttpStatus.BAD_REQUEST, "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다."),

    // ── 401 Unauthorized ──────────────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다. 다시 로그인해주세요."),
    WRONG_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),

    // ── 403 Forbidden ─────────────────────────────────────────────────
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // ── 404 Not Found ─────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 교안입니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 퀴즈입니다."),
    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // ── 409 Conflict ──────────────────────────────────────────────────
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    QUIZ_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 퀴즈입니다."),
    OAUTH2_EMAIL_ALREADY_LOCAL(HttpStatus.CONFLICT, "이미 일반 계정으로 가입된 이메일입니다."),
    SOCIAL_ACCOUNT_CONFLICT(HttpStatus.CONFLICT, "소셜 로그인으로 가입된 계정입니다."),

    // ── 500 Internal Server Error ─────────────────────────────────────
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");


    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return httpStatus.value();
    }
}