package com.capstone.eqh.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizSubmitRequestDto(
        @NotNull(message = "답안을 제출해주세요.")
        @Valid
        List<AnswerDto> answers
) {
    public record AnswerDto(
            @NotNull(message = "문제 ID가 필요합니다.")
            Long questionId,

            @NotBlank(message = "답안을 입력해주세요.")
            String studentAnswer
    ) {
    }
}
