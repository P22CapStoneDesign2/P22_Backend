package com.capstone.eqh.domain.user.dto.response;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public AuthResponseDto(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}
