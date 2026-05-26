package com.capstone.eqh.domain.quiz.dto.response;

import com.capstone.eqh.domain.quiz.entity.Quiz;

import java.time.LocalDateTime;
import java.util.List;

public record QuizDetailResponseDto(
        Long id,
        String title,
        String description,
        Long materialId,
        String materialTitle,
        Long professorId,
        String professorName,
        List<QuizQuestionResponseDto> questions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuizDetailResponseDto from(Quiz quiz) {
        return new QuizDetailResponseDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getMaterial().getId(),
                quiz.getMaterial().getTitle(),
                quiz.getProfessor().getId(),
                quiz.getProfessor().getUsername(),
                quiz.getQuestions().stream().map(QuizQuestionResponseDto::from).toList(),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt()
        );
    }
}
