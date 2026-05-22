package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentResponseDto(
        Long id,
        Long lessonId,
        String lessonTitle,
        EnrollmentStatus status,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt
) {
    public static EnrollmentResponseDto from(LessonEnrollment enrollment) {
        return new EnrollmentResponseDto(
                enrollment.getId(),
                enrollment.getLesson().getId(),
                enrollment.getLesson().getTitle(),
                enrollment.getStatus(),
                enrollment.getRequestedAt(),
                enrollment.getDecidedAt()
        );
    }
}
