package com.capstone.eqh.domain.quiz.dto.response;

import com.capstone.eqh.domain.quiz.entity.QuizQuestion;
import com.capstone.eqh.domain.quiz.enums.QuizType;

import java.util.List;

public record QuizQuestionEditResponseDto(
        Long id,
        String questionText,
        QuizType questionType,
        int score,
        String correctAnswer,
        String explanation,
        List<OptionEditResponseDto> options,
        Long anchorId,
        String anchorTitle,
        Integer lessonPage,
        Integer lessonParagraph
) {
    public record OptionEditResponseDto(Long id, String optionText, boolean correct) {
    }

    public static QuizQuestionEditResponseDto from(QuizQuestion question) {
        return new QuizQuestionEditResponseDto(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getScore(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getOptions().stream()
                        .map(opt -> new OptionEditResponseDto(opt.getId(), opt.getOptionText(), opt.isCorrect()))
                        .toList(),
                question.getAnchor() != null ? question.getAnchor().getId() : null,
                question.getAnchor() != null ? question.getAnchor().getTitle() : null,
                question.getLessonPage(),
                question.getLessonParagraph()
        );
    }
}
