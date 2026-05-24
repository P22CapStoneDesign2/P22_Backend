package com.capstone.eqh.domain.user.dto.request;

import com.capstone.eqh.domain.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequestDto(
        @NotNull(message = "status는 필수입니다.")
        UserStatus status
) {}
