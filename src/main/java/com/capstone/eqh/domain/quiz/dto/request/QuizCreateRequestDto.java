package com.capstone.eqh.domain.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuizCreateRequestDto(
        @NotBlank(message = "퀴즈 제목을 입력해주세요.")
        String title,
        String description,
        @NotNull(message = "퀴즈가 속할 교안 ID를 지정해주세요.")
<<<<<<< Updated upstream
        Long lessonId
=======
        Long materialId,
        @Valid
        List<QuizQuestionCreateRequestDto> questions
>>>>>>> Stashed changes
) {
}
