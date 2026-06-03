package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.LessonMaterial;

import java.time.LocalDateTime;

public record LessonMaterialResponseDto(
        Long id,
        Long lessonId,
        String title,
        String description,
        String fileUrl,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LessonMaterialResponseDto from(LessonMaterial material) {
        return new LessonMaterialResponseDto(
                material.getId(),
                material.getLesson().getId(),
                material.getTitle(),
                material.getDescription(),
                material.getFileUrl(),
                material.getCreatedBy() != null ? material.getCreatedBy().getId() : null,
                material.getCreatedBy() != null ? material.getCreatedBy().getUsername() : null,
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }
}
