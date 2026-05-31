package com.capstone.eqh.domain.user.password.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/password/reset-request
 */
public record PasswordResetRequestDto(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(regexp = ".+@.+\\..+", message = "올바른 이메일 형식이 아닙니다.")
        String email
) {
}
