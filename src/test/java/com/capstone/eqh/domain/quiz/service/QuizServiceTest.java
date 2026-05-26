package com.capstone.eqh.domain.quiz.service;

import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.entity.LessonMaterial;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.lesson.repository.LessonEnrollmentRepository;
import com.capstone.eqh.domain.lesson.repository.LessonMaterialRepository;
import com.capstone.eqh.domain.quiz.dto.request.QuizCreateRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizSubmitRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizUpdateRequestDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizDetailResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizSubmissionResponseDto;
import com.capstone.eqh.domain.quiz.entity.Quiz;
import com.capstone.eqh.domain.quiz.entity.QuizQuestion;
import com.capstone.eqh.domain.quiz.entity.QuizSubmission;
import com.capstone.eqh.domain.quiz.enums.QuizType;
import com.capstone.eqh.domain.quiz.repository.QuizQuestionRepository;
import com.capstone.eqh.domain.quiz.repository.QuizRepository;
import com.capstone.eqh.domain.quiz.repository.QuizSubmissionAnswerRepository;
import com.capstone.eqh.domain.quiz.repository.QuizSubmissionRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock QuizRepository quizRepository;
    @Mock QuizQuestionRepository questionRepository;
    @Mock QuizSubmissionRepository submissionRepository;
    @Mock QuizSubmissionAnswerRepository submissionAnswerRepository;
    @Mock LessonMaterialRepository materialRepository;
    @Mock LessonEnrollmentRepository enrollmentRepository;
    @Mock UserRepository userRepository;
    @InjectMocks QuizService quizService;

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
                .title("강의 " + id)
                .description("설명")
                .createdBy(professor)
                .build();
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }

    private LessonMaterial createMaterial(Long id, Lesson lesson, User professor) {
        LessonMaterial material = LessonMaterial.builder()
                .lesson(lesson)
                .title("교안 " + id)
                .description("내용")
                .createdBy(professor)
                .build();
        ReflectionTestUtils.setField(material, "id", id);
        return material;
    }

    private Quiz createQuiz(Long id, User professor, LessonMaterial material) {
        Quiz quiz = Quiz.builder()
                .professor(professor)
                .material(material)
                .title("퀴즈")
                .description("설명")
                .build();
        ReflectionTestUtils.setField(quiz, "id", id);
        return quiz;
    }

    private QuizQuestion createQuestion(Long id, Quiz quiz, String correctAnswer, int score) {
        QuizQuestion q = QuizQuestion.builder()
                .quiz(quiz)
                .questionText("문제 " + id)
                .questionType(QuizType.SHORT_ANSWER)
                .score(score)
                .correctAnswer(correctAnswer)
                .build();
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }

    @Test
    @DisplayName("create 성공: 본인 교안에 퀴즈 세트 생성")
    void create_success() {
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        QuizCreateRequestDto request = new QuizCreateRequestDto("퀴즈", "설명", 3L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(materialRepository.findById(3L)).thenReturn(Optional.of(material));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            ReflectionTestUtils.setField(q, "id", 10L);
            return q;
        });

        QuizDetailResponseDto result = quizService.create(request, 1L);

        assertThat(result.title()).isEqualTo("퀴즈");
        assertThat(result.professorId()).isEqualTo(1L);
        assertThat(result.materialId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("create 실패: 사용자가 없으면 USER_NOT_FOUND")
    void create_userNotFound() {
        QuizCreateRequestDto request = new QuizCreateRequestDto("퀴즈", "설명", 3L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.create(request, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("create 실패: 교안이 없으면 LESSON_MATERIAL_NOT_FOUND")
    void create_materialNotFound() {
        User prof = createUser(1L, Role.PROF);
        QuizCreateRequestDto request = new QuizCreateRequestDto("퀴즈", "설명", 99L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(materialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.create(request, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LESSON_MATERIAL_NOT_FOUND);
    }

    @Test
    @DisplayName("create 실패: 본인 소유 아닌 교안이면 QUIZ_LESSON_NOT_OWNED")
    void create_materialNotOwned() {
        User prof = createUser(1L, Role.PROF);
        User otherProf = createUser(2L, Role.PROF);
        Lesson lesson = createLesson(5L, otherProf);
        LessonMaterial othersMaterial = createMaterial(3L, lesson, otherProf);
        QuizCreateRequestDto request = new QuizCreateRequestDto("퀴즈", "설명", 3L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(materialRepository.findById(3L)).thenReturn(Optional.of(othersMaterial));

        assertThatThrownBy(() -> quizService.create(request, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_LESSON_NOT_OWNED);
    }

    @Test
    @DisplayName("update 실패: 퀴즈가 없으면 QUIZ_NOT_FOUND")
    void update_notFound() {
        QuizUpdateRequestDto request = new QuizUpdateRequestDto("새 제목", "새 설명");
        when(quizRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.update(99L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_NOT_FOUND);
    }

    @Test
    @DisplayName("isOwner: 출제 교수 ID와 일치하면 true")
    void isOwner_true() {
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));

        assertThat(quizService.isOwner(10L, 1L)).isTrue();
    }

    @Test
    @DisplayName("isOwner: 출제 교수 ID와 다르면 false")
    void isOwner_notOwner() {
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));

        assertThat(quizService.isOwner(10L, 999L)).isFalse();
    }

    @Test
    @DisplayName("isOwner: 퀴즈가 없으면 false")
    void isOwner_quizNotFound() {
        when(quizRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(quizService.isOwner(99L, 1L)).isFalse();
    }

    @Test
    @DisplayName("getOne 성공: USER가 APPROVED 받은 강의의 퀴즈 조회")
    void getOne_userApproved() {
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                5L, 2L, EnrollmentStatus.APPROVED)).thenReturn(true);

        assertThat(quizService.getOne(10L, 2L, Role.USER).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getOne 실패: 미승인 USER는 ENROLLMENT_NOT_APPROVED")
    void getOne_userNotApproved() {
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                5L, 2L, EnrollmentStatus.APPROVED)).thenReturn(false);

        assertThatThrownBy(() -> quizService.getOne(10L, 2L, Role.USER))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_NOT_APPROVED);
    }

    @Test
    @DisplayName("getOne 성공: PROF/ADMIN은 enrollment 검사 없이 조회")
    void getOne_profIgnoresEnrollment() {
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));

        assertThat(quizService.getOne(10L, 1L, Role.PROF).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("submit 실패: 미승인 USER는 ENROLLMENT_NOT_APPROVED")
    void submit_notApproved() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);
        QuizSubmitRequestDto request = new QuizSubmitRequestDto(
                List.of(new QuizSubmitRequestDto.AnswerDto(100L, "답")));

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                5L, 2L, EnrollmentStatus.APPROVED)).thenReturn(false);

        assertThatThrownBy(() -> quizService.submit(10L, request, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_NOT_APPROVED);
    }

    @Test
    @DisplayName("submit 실패: 이미 제출한 퀴즈면 QUIZ_ALREADY_SUBMITTED")
    void submit_alreadySubmitted() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);
        QuizSubmitRequestDto request = new QuizSubmitRequestDto(
                List.of(new QuizSubmitRequestDto.AnswerDto(100L, "답")));

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                5L, 2L, EnrollmentStatus.APPROVED)).thenReturn(true);
        when(submissionRepository.existsByQuizAndStudent(quiz, student)).thenReturn(true);

        assertThatThrownBy(() -> quizService.submit(10L, request, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_ALREADY_SUBMITTED);
    }

    @Test
    @DisplayName("submit 성공: 정답·오답 채점 후 총점 계산 (대소문자 무시)")
    void submit_success() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);
        QuizQuestion q1 = createQuestion(100L, quiz, "FCFS", 5);
        QuizQuestion q2 = createQuestion(101L, quiz, "SJF", 5);

        QuizSubmitRequestDto request = new QuizSubmitRequestDto(List.of(
                new QuizSubmitRequestDto.AnswerDto(100L, "fcfs"),
                new QuizSubmitRequestDto.AnswerDto(101L, "wrong")
        ));

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                5L, 2L, EnrollmentStatus.APPROVED)).thenReturn(true);
        when(submissionRepository.existsByQuizAndStudent(quiz, student)).thenReturn(false);
        when(questionRepository.findByQuizOrderById(quiz)).thenReturn(List.of(q1, q2));
        when(submissionRepository.save(any(QuizSubmission.class))).thenAnswer(inv -> {
            QuizSubmission s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 1000L);
            return s;
        });

        QuizSubmissionResponseDto result = quizService.submit(10L, request, 2L);

        assertThat(result.totalScore()).isEqualTo(5);
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.totalQuestions()).isEqualTo(2);
    }

    @Test
    @DisplayName("getAll(USER, materialId=null): APPROVED 강의의 교안 퀴즈만 반환")
    void getAll_userScopedToApprovedLessons() {
        Pageable pageable = PageRequest.of(0, 10);
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);

        when(enrollmentRepository.findLessonIdsByStudentIdAndStatus(2L, EnrollmentStatus.APPROVED))
                .thenReturn(List.of(5L));
        when(quizRepository.findByMaterial_Lesson_IdIn(List.of(5L), pageable))
                .thenReturn(new PageImpl<>(List.of(quiz), pageable, 1));

        Page<QuizResponseDto> result = quizService.getAll(2L, Role.USER, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(10L);
        verify(quizRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("getAll(USER, materialId=null): APPROVED 강의 없으면 빈 페이지")
    void getAll_userNoApprovedReturnsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(enrollmentRepository.findLessonIdsByStudentIdAndStatus(2L, EnrollmentStatus.APPROVED))
                .thenReturn(List.of());

        Page<QuizResponseDto> result = quizService.getAll(2L, Role.USER, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(quizRepository, never()).findByMaterial_Lesson_IdIn(any(), any());
    }

    @Test
    @DisplayName("getAll(USER, materialId=승인된 교안): 해당 교안 퀴즈 반환")
    void getAll_userWithApprovedMaterialId() {
        Pageable pageable = PageRequest.of(0, 10);
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);

        when(materialRepository.findById(3L)).thenReturn(Optional.of(material));
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                5L, 2L, EnrollmentStatus.APPROVED)).thenReturn(true);
        when(quizRepository.findByMaterial_Id(3L, pageable))
                .thenReturn(new PageImpl<>(List.of(quiz), pageable, 1));

        Page<QuizResponseDto> result = quizService.getAll(2L, Role.USER, 3L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).materialId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getAll(USER, materialId=미승인 교안): 빈 페이지 반환")
    void getAll_userWithUnapprovedMaterialIdReturnsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);

        when(materialRepository.findById(3L)).thenReturn(Optional.of(material));
        when(enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                5L, 2L, EnrollmentStatus.APPROVED)).thenReturn(false);

        Page<QuizResponseDto> result = quizService.getAll(2L, Role.USER, 3L, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(quizRepository, never()).findByMaterial_Id(any(), any());
    }

    @Test
    @DisplayName("getAll(PROF, materialId=null): 본인 출제 퀴즈 전체 반환")
    void getAll_profAllOwn() {
        Pageable pageable = PageRequest.of(0, 10);
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);

        when(quizRepository.findByProfessor_Id(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(quiz), pageable, 1));

        Page<QuizResponseDto> result = quizService.getAll(1L, Role.PROF, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(quizRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("getAll(PROF, materialId): 본인+교안 필터링")
    void getAll_profFilteredByMaterial() {
        Pageable pageable = PageRequest.of(0, 10);
        User prof = createUser(1L, Role.PROF);
        Lesson lesson = createLesson(5L, prof);
        LessonMaterial material = createMaterial(3L, lesson, prof);
        Quiz quiz = createQuiz(10L, prof, material);

        when(quizRepository.findByProfessor_IdAndMaterial_Id(1L, 3L, pageable))
                .thenReturn(new PageImpl<>(List.of(quiz), pageable, 1));

        Page<QuizResponseDto> result = quizService.getAll(1L, Role.PROF, 3L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).materialId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getAll(ADMIN, materialId=null): 전체 퀴즈 반환")
    void getAll_adminAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(quizRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<QuizResponseDto> result = quizService.getAll(99L, Role.ADMIN, null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getAll(ADMIN, materialId): 해당 교안 전체 퀴즈 반환")
    void getAll_adminFilteredByMaterial() {
        Pageable pageable = PageRequest.of(0, 10);
        when(quizRepository.findByMaterial_Id(3L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<QuizResponseDto> result = quizService.getAll(99L, Role.ADMIN, 3L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
