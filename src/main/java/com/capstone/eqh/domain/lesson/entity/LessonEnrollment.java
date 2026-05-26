package com.capstone.eqh.domain.lesson.entity;

import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lesson_enrollment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_enrollment_lesson_student",
                columnNames = {"lesson_id", "student_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonEnrollment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EnrollmentStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Builder
    private LessonEnrollment(Lesson lesson, User student) {
        this.lesson = lesson;
        this.student = student;
        this.status = EnrollmentStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(User decidedBy) {
        this.status = EnrollmentStatus.APPROVED;
        this.decidedBy = decidedBy;
        this.decidedAt = LocalDateTime.now();
    }

    public void reject(User decidedBy) {
        this.status = EnrollmentStatus.REJECTED;
        this.decidedBy = decidedBy;
        this.decidedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == EnrollmentStatus.PENDING;
    }
}
