package com.capstone.eqh.domain.quiz.dto.response;

import com.capstone.eqh.domain.quiz.entity.Quiz;

import java.time.LocalDateTime;
import java.util.List;

public record QuizEditResponseDto(
        Long id,
        String title,
        String description,
        Long materialId,
        String materialTitle,
        Long professorId,
        String professorName,
        List<QuizQuestionEditResponseDto> questions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuizEditResponseDto from(Quiz quiz) {
        return new QuizEditResponseDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getMaterial().getId(),
                quiz.getMaterial().getTitle(),
                quiz.getProfessor().getId(),
                quiz.getProfessor().getNickname(),
                quiz.getQuestions().stream().map(QuizQuestionEditResponseDto::from).toList(),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt()
        );
    }
}
