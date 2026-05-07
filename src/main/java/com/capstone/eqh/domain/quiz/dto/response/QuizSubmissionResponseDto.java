package com.capstone.eqh.domain.quiz.dto.response;

import com.capstone.eqh.domain.quiz.entity.QuizSubmission;

import java.time.LocalDateTime;
import java.util.List;

public record QuizSubmissionResponseDto(
        Long submissionId,
        Long quizId,
        String quizTitle,
        int totalScore,
        int correctCount,
        int totalQuestions,
        List<AnswerResultDto> answers,
        LocalDateTime submittedAt
) {
    public record AnswerResultDto(
            Long questionId,
            String questionText,
            String studentAnswer,
            String correctAnswer,
            boolean correct,
            int score
    ) {
    }

    public static QuizSubmissionResponseDto from(QuizSubmission submission) {
        List<AnswerResultDto> answerResults = submission.getAnswers().stream()
                .map(sa -> new AnswerResultDto(
                        sa.getQuestion().getId(),
                        sa.getQuestion().getQuestionText(),
                        sa.getStudentAnswer(),
                        sa.getQuestion().getCorrectAnswer(),
                        Boolean.TRUE.equals(sa.getCorrect()),
                        sa.getScore()
                ))
                .toList();

        long correctCount = submission.getAnswers().stream()
                .filter(sa -> Boolean.TRUE.equals(sa.getCorrect()))
                .count();

        return new QuizSubmissionResponseDto(
                submission.getId(),
                submission.getQuiz().getId(),
                submission.getQuiz().getTitle(),
                submission.getTotalScore(),
                (int) correctCount,
                submission.getAnswers().size(),
                answerResults,
                submission.getSubmittedAt()
        );
    }
}
