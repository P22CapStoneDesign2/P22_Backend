package domain.auth.exception;

/**
 * 재전송 쿨다운(최소 간격)으로 인해 아직 발송할 수 없는 경우.
 */
public class VerificationSendCooldownException extends RuntimeException {

    private final String errorCode;
    private final int retryAfterSeconds;

    public VerificationSendCooldownException(String message, String errorCode, int retryAfterSeconds) {
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
