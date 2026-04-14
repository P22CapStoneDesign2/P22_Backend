package com.capstone.eqh.domain.lesson.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LessonCreateRequestDto(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,
        String description
) {
}
