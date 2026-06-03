package com.capstone.eqh.domain.lesson.service;

import com.capstone.eqh.domain.lesson.dto.response.EnrollmentDecisionResponseDto;
import com.capstone.eqh.domain.lesson.dto.response.EnrollmentListItemResponseDto;
import com.capstone.eqh.domain.lesson.dto.response.EnrollmentResponseDto;
import com.capstone.eqh.domain.lesson.dto.response.MyLessonResponseDto;
import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.lesson.repository.LessonEnrollmentRepository;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonEnrollmentService {

    private final LessonEnrollmentRepository enrollmentRepository;
    private final LessonService lessonService;
    private final UserRepository userRepository;

    @Transactional
    public EnrollmentResponseDto request(Long lessonId, Long studentId) {
        Lesson lesson = findLesson(lessonId);
        User student = findUser(studentId);

        if (enrollmentRepository.findByLessonIdAndStudentId(lessonId, studentId).isPresent()) {
            throw new CustomException(ErrorCode.ENROLLMENT_DUPLICATE);
        }

        LessonEnrollment enrollment = LessonEnrollment.builder()
                .lesson(lesson)
                .student(student)
                .build();
        return EnrollmentResponseDto.from(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public void cancel(Long lessonId, Long studentId) {
        LessonEnrollment enrollment = enrollmentRepository
                .findByLessonIdAndStudentId(lessonId, studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.isPending()) {
            throw new CustomException(ErrorCode.ENROLLMENT_NOT_PENDING);
        }

        enrollmentRepository.delete(enrollment);
    }

    public Page<EnrollmentListItemResponseDto> listByLesson(
            Long lessonId, EnrollmentStatus status, Pageable pageable) {
        findLesson(lessonId);
        Page<LessonEnrollment> page = (status == null)
                ? enrollmentRepository.findAllByLessonId(lessonId, pageable)
                : enrollmentRepository.findAllByLessonIdAndStatus(lessonId, status, pageable);
        return page.map(EnrollmentListItemResponseDto::from);
    }

    @Transactional
    public EnrollmentDecisionResponseDto approve(Long lessonId, Long enrollmentId, Long actorId) {
        LessonEnrollment enrollment = findEnrollmentInLesson(lessonId, enrollmentId);
        if (!enrollment.isPending()) {
            throw new CustomException(ErrorCode.ENROLLMENT_NOT_PENDING);
        }
        enrollment.approve(findUser(actorId));
        return EnrollmentDecisionResponseDto.from(enrollment);
    }

    @Transactional
    public EnrollmentDecisionResponseDto reject(Long lessonId, Long enrollmentId, Long actorId) {
        LessonEnrollment enrollment = findEnrollmentInLesson(lessonId, enrollmentId);
        if (!enrollment.isPending()) {
            throw new CustomException(ErrorCode.ENROLLMENT_NOT_PENDING);
        }
        enrollment.reject(findUser(actorId));
        return EnrollmentDecisionResponseDto.from(enrollment);
    }

    public Page<MyLessonResponseDto> listMyApproved(Long studentId, Pageable pageable) {
        return enrollmentRepository
                .findAllByStudentIdAndStatus(studentId, EnrollmentStatus.APPROVED, pageable)
                .map(MyLessonResponseDto::from);
    }

    public boolean isApprovedStudent(Long lessonId, Long studentId) {
        return enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                lessonId, studentId, EnrollmentStatus.APPROVED);
    }

    private Lesson findLesson(Long lessonId) {
        return lessonService.findById(lessonId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private LessonEnrollment findEnrollmentInLesson(Long lessonId, Long enrollmentId) {
        LessonEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (!enrollment.getLesson().getId().equals(lessonId)) {
            throw new CustomException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }
        return enrollment;
    }
}
