package com.capstone.eqh.domain.user.dto.response;

import com.capstone.eqh.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        String provider,
        String role,
        LocalDateTime createdAt
) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProvider().name(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
