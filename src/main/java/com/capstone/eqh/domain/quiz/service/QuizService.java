package com.capstone.eqh.domain.quiz.service;

import com.capstone.eqh.domain.lesson.entity.LessonMaterial;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.lesson.repository.LessonEnrollmentRepository;
import com.capstone.eqh.domain.lesson.repository.LessonMaterialRepository;
import com.capstone.eqh.domain.quiz.dto.request.QuizCreateRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizQuestionCreateRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizQuestionUpdateRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizSubmitRequestDto;
import com.capstone.eqh.domain.quiz.dto.request.QuizUpdateRequestDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizDetailResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizEditResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizQuestionResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizSubmissionResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.WrongAnswerResponseDto;
import com.capstone.eqh.domain.quiz.entity.Quiz;
import com.capstone.eqh.domain.quiz.entity.QuizOption;
import com.capstone.eqh.domain.quiz.entity.QuizQuestion;
import com.capstone.eqh.domain.quiz.entity.QuizSubmission;
import com.capstone.eqh.domain.quiz.entity.QuizSubmissionAnswer;
import com.capstone.eqh.domain.quiz.enums.QuizType;
import com.capstone.eqh.domain.quiz.repository.QuizQuestionRepository;
import com.capstone.eqh.domain.quiz.repository.QuizRepository;
import com.capstone.eqh.domain.quiz.repository.QuizSubmissionAnswerRepository;
import com.capstone.eqh.domain.quiz.repository.QuizSubmissionRepository;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final QuizSubmissionAnswerRepository submissionAnswerRepository;
    private final LessonMaterialRepository materialRepository;
    private final LessonEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public QuizDetailResponseDto create(QuizCreateRequestDto request, Long professorId) {
        User professor = findUserById(professorId);
        LessonMaterial material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new CustomException(ErrorCode.LESSON_MATERIAL_NOT_FOUND));

        if (material.getCreatedBy() == null
                || !material.getCreatedBy().getId().equals(professorId)) {
            throw new CustomException(ErrorCode.QUIZ_LESSON_NOT_OWNED);
        }

        Quiz quiz = Quiz.builder()
                .professor(professor)
                .material(material)
                .title(request.title())
                .description(request.description())
                .build();
        quizRepository.save(quiz);

        if (request.questions() != null) {
            request.questions().forEach(q -> quiz.getQuestions().add(buildAndSaveQuestion(quiz, q)));
        }

        return QuizDetailResponseDto.from(quiz);
    }

    public Page<QuizResponseDto> getAll(Long userId, Role role, Long materialId, Long lessonId, Pageable pageable) {
        Page<Quiz> quizzes = switch (role) {
            case PROF -> (materialId == null)
                    ? quizRepository.findByProfessor_Id(userId, pageable)
                    : quizRepository.findByProfessor_IdAndMaterial_Id(userId, materialId, pageable);
            case ADMIN -> (materialId == null)
                    ? quizRepository.findAll(pageable)
                    : quizRepository.findByMaterial_Id(materialId, pageable);
            case USER -> findQuizzesForStudent(userId, materialId, lessonId, pageable);
        };
        return quizzes.map(QuizResponseDto::from);
    }

    private Page<Quiz> findQuizzesForStudent(Long studentId, Long materialId, Long lessonId, Pageable pageable) {
    if (materialId != null) {
        LessonMaterial material = materialRepository.findById(materialId).orElse(null);
        if (material == null) return new PageImpl<>(List.of(), pageable, 0);
        Long lid = material.getLesson().getId();
        boolean approved = enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                lid, studentId, EnrollmentStatus.APPROVED);
        if (!approved) return new PageImpl<>(List.of(), pageable, 0);
        return quizRepository.findByMaterial_Id(materialId, pageable);
    }
    if (lessonId != null) {
        boolean approved = enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                lessonId, studentId, EnrollmentStatus.APPROVED);
        if (!approved) return new PageImpl<>(List.of(), pageable, 0);
        return quizRepository.findByMaterial_Lesson_IdIn(List.of(lessonId), pageable);
    }
    List<Long> approvedLessonIds = enrollmentRepository.findLessonIdsByStudentIdAndStatus(
            studentId, EnrollmentStatus.APPROVED);
    if (approvedLessonIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
    return quizRepository.findByMaterial_Lesson_IdIn(approvedLessonIds, pageable);
}
        

    public QuizDetailResponseDto getOne(Long quizId, Long userId, Role role) {
        Quiz quiz = findQuizById(quizId);
        if (role == Role.USER) {
            assertEnrolledIfStudent(quiz, userId);
        }
        return QuizDetailResponseDto.from(quiz);
    }

    public QuizEditResponseDto getForEdit(Long quizId) {
        return QuizEditResponseDto.from(findQuizById(quizId));
    }

    @Transactional
    public QuizResponseDto update(Long quizId, QuizUpdateRequestDto request) {
        Quiz quiz = findQuizById(quizId);
        quiz.update(request.title(), request.description());
        return QuizResponseDto.from(quiz);
    }

    @Transactional
    public void delete(Long quizId) {
        quizRepository.delete(findQuizById(quizId));
    }

    @Transactional
    public QuizQuestionResponseDto addQuestion(Long quizId, QuizQuestionCreateRequestDto request) {
        Quiz quiz = findQuizById(quizId);
        return QuizQuestionResponseDto.from(buildAndSaveQuestion(quiz, request));
    }

    private QuizQuestion buildAndSaveQuestion(Quiz quiz, QuizQuestionCreateRequestDto request) {
        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .anchor(resolveAnchor(request.anchorId()))
                .questionText(request.questionText())
                .questionType(request.questionType())
                .score(request.score())
                .correctAnswer(request.correctAnswer())
                .explanation(request.explanation())
                .lessonPage(request.lessonPage())
                .lessonParagraph(request.lessonParagraph())
                .build();

        QuizQuestion saved = questionRepository.save(question);

        applyOptionsOnCreate(saved, request.questionType(), request.options());

        return saved;
    }

    @Transactional
    public QuizQuestionResponseDto updateQuestion(Long quizId, Long questionId,
                                                   QuizQuestionUpdateRequestDto request) {
        Quiz quiz = findQuizById(quizId);
        QuizQuestion question = questionRepository.findByIdAndQuiz(questionId, quiz)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        QuizType previousType = question.getQuestionType();
        QuizType newType = resolveQuestionTypeForUpdate(request, question);

        question.update(request.questionText(), request.correctAnswer(), request.explanation(),
                request.score(), request.lessonPage(), request.lessonParagraph());
        question.updateAnchor(resolveAnchor(request.anchorId()));
        question.updateQuestionType(newType);
        applyOptionsOnUpdate(question, previousType, newType, request.options());

        return QuizQuestionResponseDto.from(question);
    }

    @Transactional
    public void deleteQuestion(Long quizId, Long questionId) {
        Quiz quiz = findQuizById(quizId);
        QuizQuestion question = questionRepository.findByIdAndQuiz(questionId, quiz)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));
        questionRepository.delete(question);
    }

    @Transactional
    public QuizSubmissionResponseDto submit(Long quizId, QuizSubmitRequestDto request, Long studentId) {
        Quiz quiz = findQuizById(quizId);
        User student = findUserById(studentId);

        assertEnrolledIfStudent(quiz, studentId);

        if (submissionRepository.existsByQuizAndStudent(quiz, student)) {
            throw new CustomException(ErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        Map<Long, QuizQuestion> questionMap = questionRepository.findByQuizOrderById(quiz)
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // 1단계: 답안 채점 및 총점 계산
        record GradedAnswer(QuizQuestion question, String studentAnswer, boolean correct, int score) {}
        List<GradedAnswer> graded = new ArrayList<>();
        int totalScore = 0;

        for (QuizSubmitRequestDto.AnswerDto answerDto : request.answers()) {
            QuizQuestion question = questionMap.get(answerDto.questionId());
            if (question == null) continue;

            String correctAnswer = question.getCorrectAnswer();
            boolean correct = correctAnswer != null
                    && correctAnswer.equalsIgnoreCase(answerDto.studentAnswer().trim());
            int earnedScore = correct ? question.getScore() : 0;
            totalScore += earnedScore;
            graded.add(new GradedAnswer(question, answerDto.studentAnswer(), correct, earnedScore));
        }

        // 2단계: 최종 점수를 포함한 제출 생성
        QuizSubmission submission = QuizSubmission.builder()
                .quiz(quiz)
                .student(student)
                .totalScore(totalScore)
                .build();
        QuizSubmission savedSubmission = submissionRepository.save(submission);

        // 3단계: 문제별 답안 저장
        for (GradedAnswer ga : graded) {
            QuizSubmissionAnswer answer = QuizSubmissionAnswer.builder()
                    .submission(savedSubmission)
                    .question(ga.question())
                    .studentAnswer(ga.studentAnswer())
                    .correct(ga.correct())
                    .score(ga.score())
                    .build();
            savedSubmission.getAnswers().add(answer);
        }

        return QuizSubmissionResponseDto.from(savedSubmission);
    }

    public Page<WrongAnswerResponseDto> getWrongAnswers(Long studentId, Pageable pageable) {
        return submissionAnswerRepository
                .findWrongAnswersByStudentId(studentId, pageable)
                .map(WrongAnswerResponseDto::from);
    }

    public boolean isOwner(Long quizId, Long userId) {
        return quizRepository.findById(quizId)
                .map(quiz -> quiz.getProfessor().getId().equals(userId))
                .orElse(false);
    }

    private Quiz findQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 수정 요청에서 questionType이 없을 때:
     * - options가 빈 배열이면 단답형
     * - options에 보기가 있으면 객관식
     * - 둘 다 없으면 기존 유형 유지
     */
    private QuizType resolveQuestionTypeForUpdate(QuizQuestionUpdateRequestDto request, QuizQuestion existing) {
        if (request.questionType() != null) {
            return request.questionType();
        }
        if (request.options() != null) {
            return request.options().isEmpty() ? QuizType.SHORT_ANSWER : QuizType.MULTIPLE_CHOICE;
        }
        return existing.getQuestionType();
    }

    private void applyOptionsOnCreate(QuizQuestion question, QuizType questionType,
                                      List<QuizQuestionCreateRequestDto.OptionDto> options) {
        if (questionType == QuizType.SHORT_ANSWER) {
            return;
        }
        if (options == null || options.isEmpty()) {
            throw new CustomException(ErrorCode.QUIZ_MCQ_OPTIONS_REQUIRED);
        }
        question.replaceOptions(buildOptions(question, options));
    }

    private void applyOptionsOnUpdate(QuizQuestion question, QuizType previousType, QuizType newType,
                                      List<QuizQuestionCreateRequestDto.OptionDto> options) {
        if (newType == QuizType.SHORT_ANSWER) {
            question.replaceOptions(List.of());
            return;
        }
        if (options != null) {
            if (options.isEmpty()) {
                throw new CustomException(ErrorCode.QUIZ_MCQ_OPTIONS_REQUIRED);
            }
            question.replaceOptions(buildOptions(question, options));
            return;
        }
        if (previousType != QuizType.MULTIPLE_CHOICE) {
            throw new CustomException(ErrorCode.QUIZ_MCQ_OPTIONS_REQUIRED);
        }
    }

    private List<QuizOption> buildOptions(QuizQuestion question,
                                          List<QuizQuestionCreateRequestDto.OptionDto> optionDtos) {
        return optionDtos.stream()
                .map(opt -> QuizOption.builder()
                        .question(question)
                        .optionText(opt.optionText())
                        .correct(opt.correct())
                        .build())
                .collect(Collectors.toList());
    }

    private LessonMaterial resolveAnchor(Long anchorId) {
        if (anchorId == null) return null;
        return materialRepository.findById(anchorId)
                .orElseThrow(() -> new CustomException(ErrorCode.LESSON_MATERIAL_NOT_FOUND));
    }

    private void assertEnrolledIfStudent(Quiz quiz, Long studentId) {
        Long lessonId = quiz.getMaterial().getLesson().getId();
        boolean approved = enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                lessonId, studentId, EnrollmentStatus.APPROVED);
        if (!approved) {
            throw new CustomException(ErrorCode.ENROLLMENT_NOT_APPROVED);
        }
    }
}
