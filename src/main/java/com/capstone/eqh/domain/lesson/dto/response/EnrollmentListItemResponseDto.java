package com.capstone.eqh.domain.lesson.dto.response;

import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentListItemResponseDto(
        Long id,
        Long studentId,
        String studentName,
        String studentNickname,
        EnrollmentStatus status,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt
) {
    public static EnrollmentListItemResponseDto from(LessonEnrollment enrollment) {
        return new EnrollmentListItemResponseDto(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getUsername(),
                enrollment.getStudent().getNickname(),
                enrollment.getStatus(),
                enrollment.getRequestedAt(),
                enrollment.getDecidedAt()
        );
    }
}
