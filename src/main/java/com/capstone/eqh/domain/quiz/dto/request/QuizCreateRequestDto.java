package com.capstone.eqh.domain.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QuizCreateRequestDto(
        @NotBlank(message = "퀴즈 제목을 입력해주세요.")
        String title,
        String description
) {
}
