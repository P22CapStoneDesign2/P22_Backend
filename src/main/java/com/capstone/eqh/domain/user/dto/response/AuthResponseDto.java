package com.capstone.eqh.domain.user.dto.response;

import com.capstone.eqh.domain.user.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserStatus status
) {
    public AuthResponseDto(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer", null);
    }

    public AuthResponseDto(String accessToken, String refreshToken, UserStatus status) {
        this(accessToken, refreshToken, "Bearer", status);
    }
}
