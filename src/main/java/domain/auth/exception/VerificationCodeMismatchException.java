package domain.auth.exception;

/**
 * 인증번호 불일치로 검증에 실패한 경우.
 */
public class VerificationCodeMismatchException extends RuntimeException {

    private final String errorCode;
    private final int remainingAttempts;

    public VerificationCodeMismatchException(String message, String errorCode, int remainingAttempts) {
        super(message);
        this.errorCode = errorCode;
        this.remainingAttempts = remainingAttempts;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
