package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.Lesson;

import java.time.LocalDateTime;

public record LessonResponseDto(
        Long id,
        String title,
        String description,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LessonResponseDto from(Lesson lesson) {
        return new LessonResponseDto(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getCreatedBy() != null ? lesson.getCreatedBy().getId() : null,
                lesson.getCreatedBy() != null ? lesson.getCreatedBy().getUsername() : null,
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }
}
