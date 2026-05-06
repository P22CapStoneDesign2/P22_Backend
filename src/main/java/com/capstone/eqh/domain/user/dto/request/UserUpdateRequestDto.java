package com.capstone.eqh.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDto(
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String username,

        String currentPassword,

        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
                message = "비밀번호는 8~20자, 영문+숫자+특수문자를 포함해야 합니다."
        )
        String newPassword
) {}
