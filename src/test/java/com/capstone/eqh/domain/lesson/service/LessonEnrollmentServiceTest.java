package com.capstone.eqh.domain.lesson.service;

import com.capstone.eqh.domain.lesson.dto.response.EnrollmentDecisionResponseDto;
import com.capstone.eqh.domain.lesson.dto.response.EnrollmentResponseDto;
import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.lesson.repository.LessonEnrollmentRepository;
import com.capstone.eqh.domain.lesson.service.LessonService;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonEnrollmentServiceTest {

    @Mock LessonEnrollmentRepository enrollmentRepository;
    @Mock LessonService lessonService;
    @Mock UserRepository userRepository;
    @InjectMocks LessonEnrollmentService service;

    private User createUser(Long id, Role role) {
        User user = User.builder()
                .username("user" + id)
                .nickname("nick" + id)
                .email("u" + id + "@test.com")
                .provider(AuthProvider.LOCAL)
                .role(role)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Lesson createLesson(Long id, User professor) {
        Lesson lesson = Lesson.builder()
                .title("교안 " + id)
                .description("내용")
                .createdBy(professor)
                .build();
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }

    private LessonEnrollment createEnrollment(Long id, Lesson lesson, User student) {
        LessonEnrollment e = LessonEnrollment.builder()
                .lesson(lesson)
                .student(student)
                .build();
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    @Test
    @DisplayName("request 성공: 신규 신청은 PENDING으로 저장")
    void request_success() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(3L, prof);

        when(lessonService.findById(3L)).thenReturn(lesson);
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByLessonIdAndStudentId(3L, 2L)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(LessonEnrollment.class))).thenAnswer(inv -> {
            LessonEnrollment e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 42L);
            return e;
        });

        EnrollmentResponseDto result = service.request(3L, 2L);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.lessonId()).isEqualTo(3L);
        assertThat(result.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.decidedAt()).isNull();
    }

    @Test
    @DisplayName("request 실패: 동일 학생-교안 재신청은 ENROLLMENT_DUPLICATE")
    void request_duplicate() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(3L, prof);
        LessonEnrollment existing = createEnrollment(99L, lesson, student);

        when(lessonService.findById(3L)).thenReturn(lesson);
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByLessonIdAndStudentId(3L, 2L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.request(3L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_DUPLICATE);
    }

    @Test
    @DisplayName("request 실패: 교안이 없으면 LESSON_NOT_FOUND")
    void request_lessonNotFound() {
        when(lessonService.findById(99L)).thenThrow(new CustomException(ErrorCode.LESSON_NOT_FOUND));

        assertThatThrownBy(() -> service.request(99L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LESSON_NOT_FOUND);
    }

    @Test
    @DisplayName("cancel 성공: PENDING 상태 본인 신청 삭제")
    void cancel_success() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(3L, prof);
        LessonEnrollment existing = createEnrollment(42L, lesson, student);

        when(enrollmentRepository.findByLessonIdAndStudentId(3L, 2L)).thenReturn(Optional.of(existing));

        service.cancel(3L, 2L);
    }

    @Test
    @DisplayName("cancel 실패: APPROVED 상태는 ENROLLMENT_NOT_PENDING")
    void cancel_alreadyApproved() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(3L, prof);
        LessonEnrollment existing = createEnrollment(42L, lesson, student);
        existing.approve(prof);

        when(enrollmentRepository.findByLessonIdAndStudentId(3L, 2L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.cancel(3L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_NOT_PENDING);
    }

    @Test
    @DisplayName("cancel 실패: 신청이 없으면 ENROLLMENT_NOT_FOUND")
    void cancel_notFound() {
        when(enrollmentRepository.findByLessonIdAndStudentId(3L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(3L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("approve 성공: PENDING이면 APPROVED로 변경하고 decidedAt 기록")
    void approve_success() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(3L, prof);
        LessonEnrollment existing = createEnrollment(42L, lesson, student);

        when(enrollmentRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(prof));

        EnrollmentDecisionResponseDto result = service.approve(3L, 42L, 1L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.APPROVED);
        assertThat(result.decidedAt()).isNotNull();
    }

    @Test
    @DisplayName("approve 실패: 이미 결정된 신청은 ENROLLMENT_NOT_PENDING")
    void approve_alreadyDecided() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(3L, prof);
        LessonEnrollment existing = createEnrollment(42L, lesson, student);
        existing.reject(prof);

        when(enrollmentRepository.findById(42L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.approve(3L, 42L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_NOT_PENDING);
    }

    @Test
    @DisplayName("approve 실패: enrollment의 lessonId가 path와 다르면 ENROLLMENT_NOT_FOUND")
    void approve_lessonMismatch() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson otherLesson = createLesson(7L, prof);
        LessonEnrollment existing = createEnrollment(42L, otherLesson, student);

        when(enrollmentRepository.findById(42L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.approve(3L, 42L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("reject 성공: PENDING이면 REJECTED로 변경하고 decidedAt 기록")
    void reject_success() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(3L, prof);
        LessonEnrollment existing = createEnrollment(42L, lesson, student);

        when(enrollmentRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(prof));

        EnrollmentDecisionResponseDto result = service.reject(3L, 42L, 1L);

        assertThat(result.status()).isEqualTo(EnrollmentStatus.REJECTED);
        assertThat(result.decidedAt()).isNotNull();
    }

    @Test
    @DisplayName("isApprovedStudent: APPROVED 행이 있으면 true")
    void isApprovedStudent_true() {
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                3L, 2L, EnrollmentStatus.APPROVED)).thenReturn(true);

        assertThat(service.isApprovedStudent(3L, 2L)).isTrue();
    }

    @Test
    @DisplayName("isApprovedStudent: APPROVED 행이 없으면 false")
    void isApprovedStudent_false() {
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                3L, 2L, EnrollmentStatus.APPROVED)).thenReturn(false);

        assertThat(service.isApprovedStudent(3L, 2L)).isFalse();
    }
}
