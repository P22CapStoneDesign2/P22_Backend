package domain.auth.exception;

/**
 * 인증번호 검증 실패 횟수가 한도를 초과해 계정(이메일) 단위 잠금이 걸린 경우.
 */
public class VerificationAttemptsExceededException extends RuntimeException {

    private final String errorCode;
    private final int retryAfterSeconds;

    public VerificationAttemptsExceededException(String message, String errorCode, int retryAfterSeconds) {
        super(message);
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
