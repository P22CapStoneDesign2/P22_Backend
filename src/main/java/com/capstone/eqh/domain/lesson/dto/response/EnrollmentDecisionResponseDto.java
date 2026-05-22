package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentDecisionResponseDto(
        Long id,
        Long studentId,
        EnrollmentStatus status,
        LocalDateTime decidedAt
) {
    public static EnrollmentDecisionResponseDto from(LessonEnrollment enrollment) {
        return new EnrollmentDecisionResponseDto(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStatus(),
                enrollment.getDecidedAt()
        );
    }
}
