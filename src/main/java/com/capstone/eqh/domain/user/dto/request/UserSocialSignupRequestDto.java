package com.capstone.eqh.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSocialSignupRequestDto(

        @NotBlank(message = "소셜 가입 토큰은 필수입니다.")
        String pendingToken,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 20, message = "이름은 2~20자여야 합니다.")
        String username,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣]+$", message = "닉네임은 영문, 숫자, 한글만 사용 가능합니다.")
        String nickname
) {}
