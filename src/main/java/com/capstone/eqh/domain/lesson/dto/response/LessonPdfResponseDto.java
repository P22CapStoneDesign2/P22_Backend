package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.LessonPdf;

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
    public static LessonPdfResponseDto from(LessonPdf lessonPdf) {
        return new LessonPdfResponseDto(
                lessonPdf.getId(),
                lessonPdf.getLesson().getId(),
                lessonPdf.getOriginalFileName(),
                lessonPdf.getSavedFileName(),
                lessonPdf.getFileUrl(),
                lessonPdf.getFileSize(),
                lessonPdf.getUploadedBy().getId(),
                lessonPdf.getCreatedAt()
        );
    }
}
