package com.capstone.eqh.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record QuizQuestionUpdateRequestDto(
        @NotBlank(message = "문제를 입력해주세요.")
        String questionText,

        @Valid
        List<QuizQuestionCreateRequestDto.OptionDto> options,

        @NotBlank(message = "정답을 입력해주세요.")
        String correctAnswer,

        String explanation,

        @Min(value = 0, message = "배점은 0점 이상이어야 합니다.")
        int score,

        Long anchorId,

        Integer lessonPage,

        Integer lessonParagraph
) {
}
