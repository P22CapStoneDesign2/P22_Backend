package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.LessonMaterial;

import java.time.LocalDateTime;

public record LessonPdfResponseDto(
        Long pdfId,
        Long lessonId,
        String fileUrl,
        Long uploadedById,
        LocalDateTime uploadedAt
) {
    public static LessonPdfResponseDto from(LessonMaterial material) {
        return new LessonPdfResponseDto(
                material.getId(),
                material.getLesson().getId(),
                material.getFileUrl(),
                material.getCreatedBy() != null ? material.getCreatedBy().getId() : null,
                material.getCreatedAt()
        );
    }
}