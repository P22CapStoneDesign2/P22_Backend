package global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 실패 응답의 data 영역에 담기는 표준 에러 페이로드.
 * 클라이언트가 errorCode로 분기 처리할 수 있다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorBody {

    private final String errorCode;
    private final Integer retryAfterSeconds;
    private final Integer remainingAttempts;

    public ErrorBody(String errorCode, Integer retryAfterSeconds, Integer remainingAttempts) {
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
        this.remainingAttempts = remainingAttempts;
    }

    public static ErrorBody of(String errorCode) {
        return new ErrorBody(errorCode, null, null);
    }

    public static ErrorBody withRetry(String errorCode, int retryAfterSeconds) {
        return new ErrorBody(errorCode, retryAfterSeconds, null);
    }

    public static ErrorBody withAttempts(String errorCode, int remainingAttempts) {
        return new ErrorBody(errorCode, null, remainingAttempts);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }
}
