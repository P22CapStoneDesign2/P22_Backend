package com.capstone.eqh.domain.user.dto.response;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.UserStatus;

public record ProfessorStatusResponseDto(
        Long id,
        UserStatus status
) {
    public static ProfessorStatusResponseDto from(User user) {
        return new ProfessorStatusResponseDto(user.getId(), user.getStatus());
    }
}
