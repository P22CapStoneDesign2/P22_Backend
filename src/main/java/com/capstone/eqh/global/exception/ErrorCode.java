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
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 비어 있거나 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
    SOCIAL_CANNOT_CHANGE_PASSWORD(HttpStatus.BAD_REQUEST, "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다."),

    INVALID_PDF_TYPE(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드할 수 있습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기는 50MB를 초과할 수 없습니다."),

    ENROLLMENT_NOT_PENDING(HttpStatus.BAD_REQUEST, "대기 중인 신청이 아닙니다."),


    // ── 401 Unauthorized ──────────────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다. 다시 로그인해주세요."),
    INVALID_PENDING_TOKEN(HttpStatus.UNAUTHORIZED, "소셜 가입 정보가 만료되었거나 유효하지 않습니다. 카카오 로그인을 다시 시도해 주세요."),
    WRONG_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),

    // ── 403 Forbidden ─────────────────────────────────────────────────
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증이 완료되지 않았습니다."),
    ENROLLMENT_NOT_APPROVED(HttpStatus.FORBIDDEN, "수강 승인되지 않은 강의입니다."),
    QUIZ_LESSON_NOT_OWNED(HttpStatus.FORBIDDEN, "본인 소유의 교안에만 퀴즈를 생성할 수 있습니다."),
    LESSON_MATERIAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "수강 중인 강의의 교안만 조회할 수 있습니다."),
    PROF_NOT_APPROVED(HttpStatus.FORBIDDEN, "교수 계정 승인 대기 중입니다."),

    // ── 404 Not Found ─────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 교안입니다."),
    PDF_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 PDF입니다."),

    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 강의입니다."),
    LESSON_MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 교안입니다."),
    LESSON_MATERIAL_NOT_IN_LESSON(HttpStatus.NOT_FOUND, "해당 강의에 속한 교안이 아닙니다."),

    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 퀴즈입니다."),
    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청을 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    VERIFICATION_NOT_FOUND_OR_EXPIRED(HttpStatus.NOT_FOUND, "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해 주세요."),

    // ── 409 Conflict ──────────────────────────────────────────────────
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    EMAIL_REJECTED(HttpStatus.CONFLICT, "가입이 거절된 이메일입니다. 관리자에게 문의해 주세요."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    QUIZ_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 퀴즈입니다."),
    ENROLLMENT_DUPLICATE(HttpStatus.CONFLICT, "이미 신청한 강의입니다."),
    OAUTH2_EMAIL_ALREADY_LOCAL(HttpStatus.CONFLICT, "이미 일반 계정으로 가입된 이메일입니다."),
    SOCIAL_ACCOUNT_CONFLICT(HttpStatus.CONFLICT, "소셜 로그인으로 가입된 계정입니다."),

    // ── 423 Locked ────────────────────────────────────────────────────
    VERIFICATION_LOCKED(HttpStatus.LOCKED, "인증 시도가 잠금 상태입니다. 잠시 후 다시 시도해 주세요."),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.LOCKED, "인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // ── 429 Too Many Requests ─────────────────────────────────────────
    VERIFICATION_SEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "재전송 대기 시간이 남아 있습니다. 잠시 후 다시 시도해 주세요."),
    VERIFICATION_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증번호 발송 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // ── 401 Unauthorized (verification mismatch) ──────────────────────
    VERIFICATION_CODE_MISMATCH(HttpStatus.UNAUTHORIZED, "인증번호가 일치하지 않습니다."),

    // ── 500 Internal Server Error ─────────────────────────────────────
    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    STORAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");


    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return httpStatus.value();
    }
}