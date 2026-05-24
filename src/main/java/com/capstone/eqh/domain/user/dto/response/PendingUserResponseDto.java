package com.capstone.eqh.domain.user.dto.response;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

public record PendingUserResponseDto(
        Long id,
        String username,
        String email,
        String nickname,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static PendingUserResponseDto from(User user) {
        return new PendingUserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
