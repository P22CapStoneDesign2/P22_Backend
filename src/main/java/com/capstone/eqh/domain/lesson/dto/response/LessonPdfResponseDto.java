package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.material.entity.Material;

import java.time.LocalDateTime;

public record LessonPdfResponseDto(
        Long pdfId,
        Long lessonId,
        String originalFileName,
        String savedFileName,
        String fileUrl,
        Long fileSize,
        Long uploadedById,
        LocalDateTime uploadedAt
) {
    public static LessonPdfResponseDto from(Material material) {
        return new LessonPdfResponseDto(
                material.getId(),
                material.getLesson().getId(),
                material.getOriginalFileName(),
                material.getSavedFileName(),
                material.getPdfUrl(),
                material.getFileSize(),
                material.getUploadedBy().getId(),
                material.getCreatedAt()
        );
    }
}
