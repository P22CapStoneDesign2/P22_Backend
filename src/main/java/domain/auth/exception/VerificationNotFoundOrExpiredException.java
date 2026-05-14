package domain.auth.exception;

/**
 * 저장된 인증번호가 없거나 TTL 만료로 삭제된 경우.
 */
public class VerificationNotFoundOrExpiredException extends RuntimeException {

    private final String errorCode;

    public VerificationNotFoundOrExpiredException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
