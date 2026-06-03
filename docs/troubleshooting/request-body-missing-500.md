# 요청 본문 누락 시 500 (HttpMessageNotReadableException 미처리)

- **발생일**: 2026-05-25
- **발견 경로**: 프론트엔드/Postman 연동 테스트 (`POST /api/lessons` body 누락 호출)
- **관련 파일**:
  - `global/exception/GlobalExceptionHandler.java`
  - `global/exception/ErrorCode.java`

## 증상

`POST /api/lessons` 호출 시 body 가 비어있거나 `Content-Type: application/json` 이 아닌 경우 500 응답.
응답 메시지: "서버 내부 오류가 발생했습니다."

백엔드 로그:

```
[UnhandledException] Required request body is missing: ... LessonController.create(...)
org.springframework.http.converter.HttpMessageNotReadableException: Required request body is missing
```

## 원인

`@RequestBody` 인자 파싱 실패 시 Spring MVC 는 `HttpMessageNotReadableException` 을 던지지만, `GlobalExceptionHandler` 에 전용 핸들러가 없어서 catch-all `@ExceptionHandler(Exception.class)` 가 잡아 `INTERNAL_SERVER_ERROR` (500) 로 변환됐다.

CLAUDE.md 원칙상 클라이언트 요청 오류는 400 + 한국어 `ApiResponse` 로 일관되게 반환해야 한다.

## 조치

1. `ErrorCode.INVALID_REQUEST_BODY` 추가 (400, "요청 본문이 비어 있거나 형식이 올바르지 않습니다.").
2. `GlobalExceptionHandler` 에 `HttpMessageNotReadableException` 전용 핸들러 추가 — catch-all 위에 배치해 500 으로 새지 않도록 한다.

핵심 변경:

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
    log.warn("[HttpMessageNotReadableException] {}", e.getMessage());
    ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
    return ResponseEntity
            .status(errorCode.getStatusCode())
            .body(ApiResponse.failure(errorCode.getStatusCode(), errorCode.getMessage()));
}
```

## 검증

`./gradlew build` 통과. 이후 body 없이 `POST /api/lessons` 호출 시 400 + `INVALID_REQUEST_BODY` 메시지 응답을 확인한다.

## 교훈 / 재발 방지

- Spring MVC 의 클라이언트 요청 관련 예외(`HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` 등)는 모두 400 대로 매핑되어야 한다. 비슷한 케이스가 또 발견되면 동일 패턴으로 핸들러를 추가한다.
- catch-all `@ExceptionHandler(Exception.class)` 는 항상 가장 마지막에 위치시키고, 더 구체적인 예외를 위에 명시적으로 처리해 500 누수를 막는다.
