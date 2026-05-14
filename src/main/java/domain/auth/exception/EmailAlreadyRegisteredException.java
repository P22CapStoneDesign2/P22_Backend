package domain.auth.exception;

/**
 * 이미 등록된 이메일로 인증번호를 요청한 경우.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    private final String errorCode;

    public EmailAlreadyRegisteredException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
