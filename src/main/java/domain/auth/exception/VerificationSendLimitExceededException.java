package domain.auth.exception;

/**
 * 시간당 인증번호 발송 횟수가 한도를 초과한 경우.
 */
public class VerificationSendLimitExceededException extends RuntimeException {

    private final String errorCode;
    private final int retryAfterSeconds;

    public VerificationSendLimitExceededException(String message, String errorCode, int retryAfterSeconds) {
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
