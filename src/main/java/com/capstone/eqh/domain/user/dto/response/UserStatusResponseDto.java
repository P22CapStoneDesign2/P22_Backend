package com.capstone.eqh.domain.user.dto.response;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.UserStatus;

public record UserStatusResponseDto(
        Long id,
        UserStatus status
) {
    public static UserStatusResponseDto from(User user) {
        return new UserStatusResponseDto(user.getId(), user.getStatus());
    }
}
