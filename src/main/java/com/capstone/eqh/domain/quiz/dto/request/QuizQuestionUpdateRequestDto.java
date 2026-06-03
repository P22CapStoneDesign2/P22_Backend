package com.capstone.eqh.domain.quiz.dto.request;

import com.capstone.eqh.domain.quiz.enums.QuizType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record QuizQuestionUpdateRequestDto(
        @NotBlank(message = "문제를 입력해주세요.")
        String questionText,

        /** null이면 options 유무로 유형 추론, 없으면 기존 유형 유지 */
        @JsonAlias({"type", "question_type", "qType", "q_type"})
        QuizType questionType,

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
