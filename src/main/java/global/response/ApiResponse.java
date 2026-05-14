package global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * REST API 공통 응답 래퍼.
 * 성공: success, message, data(null 생략 가능)
 * 실패: success, message, errorCode (루트 필드)
 *
 * @param <T> data 페이로드 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String errorCode;

    private ApiResponse(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * data 없이 성공만 알릴 때 사용 (예: 인증번호 발송 완료).
     */
    public static ApiResponse<Void> okWithoutData(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * 성공 응답(data 없음). {@link #okWithoutData(String)} 과 동일한 의미의 별칭.
     */
    public static ApiResponse<Void> success(String message) {
        return okWithoutData(message);
    }

    /**
     * 실패 응답. errorCode는 JSON 루트에 포함되며 data는 null이다.
     */
    public static ApiResponse<Void> failure(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }

    /**
     * 하위 호환용. 신규 코드는 {@link #failure(String, String)} 사용을 권장한다.
     */
    public static <T> ApiResponse<T> fail(String message, T data) {
        return new ApiResponse<>(false, message, data, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
