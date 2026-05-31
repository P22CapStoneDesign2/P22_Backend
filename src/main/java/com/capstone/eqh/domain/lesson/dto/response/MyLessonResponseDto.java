package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;

import java.time.LocalDateTime;

public record MyLessonResponseDto(
        Long id,
        String title,
        String description,
        Long createdById,
        String createdByName,
        LocalDateTime approvedAt
) {
    public static MyLessonResponseDto from(LessonEnrollment enrollment) {
        Lesson lesson = enrollment.getLesson();
        Long createdById = lesson.getCreatedBy() != null ? lesson.getCreatedBy().getId() : null;
        String createdByName = lesson.getCreatedBy() != null ? lesson.getCreatedBy().getUsername() : null;
        return new MyLessonResponseDto(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                createdById,
                createdByName,
                enrollment.getDecidedAt()
        );
    }
}
