package domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 인증번호 발송 요청 DTO.
 */
public record EmailSendRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {
}
