package com.capstone.eqh.domain.quiz.dto.request;

import com.capstone.eqh.domain.quiz.enums.QuizType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizQuestionCreateRequestDto(
        @NotBlank(message = "문제를 입력해주세요.")
        String questionText,

        @NotNull(message = "문제 유형을 선택해주세요.")
        QuizType questionType,

        @Valid
        List<OptionDto> options,

        @NotBlank(message = "정답을 입력해주세요.")
        String correctAnswer,

        String explanation,

        @Min(value = 0, message = "배점은 0점 이상이어야 합니다.")
        int score,

        Long anchorId,

        Integer lessonPage,

        Integer lessonParagraph
) {
    public record OptionDto(
            @NotBlank(message = "보기 내용을 입력해주세요.")
            String optionText,
            boolean correct
    ) {
    }
}
