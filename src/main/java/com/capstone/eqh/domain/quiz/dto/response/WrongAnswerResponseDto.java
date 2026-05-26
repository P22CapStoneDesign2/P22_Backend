package com.capstone.eqh.domain.quiz.dto.response;

import com.capstone.eqh.domain.quiz.entity.QuizSubmissionAnswer;
import com.capstone.eqh.domain.quiz.enums.QuizType;

import java.time.LocalDateTime;
import java.util.List;

public record WrongAnswerResponseDto(
        Long submissionId,
        Long quizId,
        String quizTitle,
        Long questionId,
        String questionText,
        QuizType questionType,
        List<String> options,
        String studentAnswer,
        String correctAnswer,
        String explanation,
        LessonRefDto lessonRef,
        LocalDateTime submittedAt
) {
    public record LessonRefDto(
            Long lessonId,
            String lessonTitle,
            Integer lessonPage,
            Integer lessonParagraph
    ) {
    }

    public static WrongAnswerResponseDto from(QuizSubmissionAnswer sa) {
        var question = sa.getQuestion();
        var lesson = question.getAnchor();

        LessonRefDto lessonRef = lesson != null
                ? new LessonRefDto(lesson.getId(), lesson.getTitle(), question.getLessonPage(), question.getLessonParagraph())
                : null;

        List<String> options = question.getOptions().stream()
                .map(opt -> opt.getOptionText())
                .toList();

        return new WrongAnswerResponseDto(
                sa.getSubmission().getId(),
                sa.getSubmission().getQuiz().getId(),
                sa.getSubmission().getQuiz().getTitle(),
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                options,
                sa.getStudentAnswer(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                lessonRef,
                sa.getSubmission().getSubmittedAt()
        );
    }
}
