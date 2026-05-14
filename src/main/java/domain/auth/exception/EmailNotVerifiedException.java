package domain.auth.exception;

/**
 * 회원가입 시점에 이메일 인증이 완료되지 않았거나 TTL이 만료된 경우.
 * 다른 서비스(회원가입 서비스)에서 호출해 검증한다.
 */
public class EmailNotVerifiedException extends RuntimeException {

    private final String errorCode;

    public EmailNotVerifiedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
