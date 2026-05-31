package com.capstone.eqh.domain.quiz.dto.response;

import com.capstone.eqh.domain.quiz.entity.Quiz;

import java.time.LocalDateTime;

public record QuizResponseDto(
        Long id,
        String title,
        String description,
        Long materialId,
        String materialTitle,
        Long professorId,
        String professorName,
        int questionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuizResponseDto from(Quiz quiz) {
        return new QuizResponseDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getMaterial().getId(),
                quiz.getMaterial().getTitle(),
                quiz.getProfessor().getId(),
                quiz.getProfessor().getUsername(),
                quiz.getQuestions().size(),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt()
        );
    }
}
