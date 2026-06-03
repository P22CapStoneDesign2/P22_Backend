package com.capstone.eqh.domain.quiz.dto.response;

import com.capstone.eqh.domain.quiz.entity.QuizQuestion;
import com.capstone.eqh.domain.quiz.enums.QuizType;

import java.util.List;

public record QuizQuestionResponseDto(
        Long id,
        String questionText,
        QuizType questionType,
        int score,
        List<OptionResponseDto> options,
        Long anchorId,
        String anchorTitle,
        Integer lessonPage,
        Integer lessonParagraph,
        String explanation
) {
    public record OptionResponseDto(Long id, String optionText) {
    }

    public static QuizQuestionResponseDto from(QuizQuestion question) {
        return new QuizQuestionResponseDto(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getScore(),
                question.getOptions().stream()
                        .map(opt -> new OptionResponseDto(opt.getId(), opt.getOptionText()))
                        .toList(),
                question.getAnchor() != null ? question.getAnchor().getId() : null,
                question.getAnchor() != null ? question.getAnchor().getTitle() : null,
                question.getLessonPage(),
                question.getLessonParagraph(),
                question.getExplanation()
        );
    }
}
