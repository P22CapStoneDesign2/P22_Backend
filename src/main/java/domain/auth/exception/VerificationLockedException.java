package domain.auth.exception;

/**
 * 인증번호 검증이 잠금(ev:lock) 상태인 경우.
 * (최대 실패 횟수 초과 등으로 잠긴 이메일)
 */
public class VerificationLockedException extends RuntimeException {

    private final String errorCode;
    private final int retryAfterSeconds;

    public VerificationLockedException(String message, String errorCode, int retryAfterSeconds) {
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
