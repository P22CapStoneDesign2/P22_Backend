package com.capstone.eqh.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {}
