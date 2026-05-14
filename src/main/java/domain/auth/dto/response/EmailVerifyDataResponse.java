package domain.auth.dto.response;

/**
 * 인증 성공 응답의 data 영역.
 */
public record EmailVerifyDataResponse(boolean verified) {

    /** 인증 완료({@code verified=true}). record의 {@code verified()} 접근자와 static 메서드명 충돌을 피한다. */
    public static EmailVerifyDataResponse success() {
        return new EmailVerifyDataResponse(true);
    }
}
