package com.capstone.eqh.domain.quiz.service;

import com.capstone.eqh.domain.lesson.repository.LessonRepository;
import com.capstone.eqh.domain.quiz.dto.request.QuizCreateRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizSubmitRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizUpdateRequestDto;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock QuizRepository quizRepository;
    @Mock QuizQuestionRepository questionRepository;
    @Mock QuizSubmissionRepository submissionRepository;
    @Mock QuizSubmissionAnswerRepository submissionAnswerRepository;
    @Mock LessonRepository lessonRepository;
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

    private Quiz createQuiz(Long id, User professor) {
        Quiz quiz = Quiz.builder()
                .professor(professor)
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
    @DisplayName("create 성공: 퀴즈 세트 생성")
    void create_success() {
        QuizCreateRequestDto request = new QuizCreateRequestDto("퀴즈", "설명");
        User prof = createUser(1L, Role.PROF);
        when(userRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            ReflectionTestUtils.setField(q, "id", 10L);
            return q;
        });

        QuizResponseDto result = quizService.create(request, 1L);

        assertThat(result.title()).isEqualTo("퀴즈");
        assertThat(result.professorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("create 실패: 사용자가 없으면 USER_NOT_FOUND")
    void create_userNotFound() {
        QuizCreateRequestDto request = new QuizCreateRequestDto("퀴즈", "설명");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.create(request, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
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
        Quiz quiz = createQuiz(10L, prof);
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));

        assertThat(quizService.isOwner(10L, 1L)).isTrue();
    }

    @Test
    @DisplayName("isOwner: 출제 교수 ID와 다르면 false")
    void isOwner_notOwner() {
        User prof = createUser(1L, Role.PROF);
        Quiz quiz = createQuiz(10L, prof);
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
    @DisplayName("submit 실패: 이미 제출한 퀴즈면 QUIZ_ALREADY_SUBMITTED")
    void submit_alreadySubmitted() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Quiz quiz = createQuiz(10L, prof);
        QuizSubmitRequestDto request = new QuizSubmitRequestDto(
                List.of(new QuizSubmitRequestDto.AnswerDto(100L, "답"))
        );

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
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
        Quiz quiz = createQuiz(10L, prof);
        QuizQuestion q1 = createQuestion(100L, quiz, "FCFS", 5);
        QuizQuestion q2 = createQuestion(101L, quiz, "SJF", 5);

        QuizSubmitRequestDto request = new QuizSubmitRequestDto(List.of(
                new QuizSubmitRequestDto.AnswerDto(100L, "fcfs"),  // 정답 (대소문자 무시)
                new QuizSubmitRequestDto.AnswerDto(101L, "wrong")  // 오답
        ));

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
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
    @DisplayName("submit: 알 수 없는 questionId는 무시")
    void submit_ignoresUnknownQuestion() {
        User prof = createUser(1L, Role.PROF);
        User student = createUser(2L, Role.USER);
        Quiz quiz = createQuiz(10L, prof);
        QuizQuestion q1 = createQuestion(100L, quiz, "FCFS", 5);

        QuizSubmitRequestDto request = new QuizSubmitRequestDto(List.of(
                new QuizSubmitRequestDto.AnswerDto(100L, "FCFS"),
                new QuizSubmitRequestDto.AnswerDto(999L, "unknown")  // 존재하지 않는 문제
        ));

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(submissionRepository.existsByQuizAndStudent(quiz, student)).thenReturn(false);
        when(questionRepository.findByQuizOrderById(quiz)).thenReturn(List.of(q1));
        when(submissionRepository.save(any(QuizSubmission.class))).thenAnswer(inv -> {
            QuizSubmission s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 1000L);
            return s;
        });

        QuizSubmissionResponseDto result = quizService.submit(10L, request, 2L);

        assertThat(result.totalScore()).isEqualTo(5);
        assertThat(result.totalQuestions()).isEqualTo(1);  // 알 수 없는 문제는 건너뜀
    }
}
