package com.capstone.eqh.domain.user.dto.response;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

public record PendingProfessorResponseDto(
        Long id,
        String username,
        String email,
        String nickname,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static PendingProfessorResponseDto from(User user) {
        return new PendingProfessorResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
