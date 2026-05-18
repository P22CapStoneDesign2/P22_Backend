package com.capstone.eqh.domain.quiz.service;

import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.repository.LessonRepository;
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
import com.capstone.eqh.domain.quiz.repository.QuizQuestionRepository;
import com.capstone.eqh.domain.quiz.repository.QuizRepository;
import com.capstone.eqh.domain.quiz.repository.QuizSubmissionAnswerRepository;
import com.capstone.eqh.domain.quiz.repository.QuizSubmissionRepository;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import com.capstone.eqh.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Transactional
    public QuizResponseDto create(QuizCreateRequestDto request, Long professorId) {
        User professor = findUserById(professorId);
        Quiz quiz = Quiz.builder()
                .professor(professor)
                .title(request.title())
                .description(request.description())
                .build();
        return QuizResponseDto.from(quizRepository.save(quiz));
    }

    public Page<QuizResponseDto> getAll(Pageable pageable) {
        return quizRepository.findAll(pageable).map(QuizResponseDto::from);
    }

    public QuizDetailResponseDto getOne(Long quizId) {
        return QuizDetailResponseDto.from(findQuizById(quizId));
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

        if (request.options() != null && !request.options().isEmpty()) {
            List<QuizOption> options = request.options().stream()
                    .map(opt -> QuizOption.builder()
                            .question(saved)
                            .optionText(opt.optionText())
                            .correct(opt.correct())
                            .build())
                    .collect(Collectors.toList());
            saved.replaceOptions(options);
        }

        return QuizQuestionResponseDto.from(saved);
    }

    @Transactional
    public QuizQuestionResponseDto updateQuestion(Long quizId, Long questionId,
                                                   QuizQuestionUpdateRequestDto request) {
        Quiz quiz = findQuizById(quizId);
        QuizQuestion question = questionRepository.findByIdAndQuiz(questionId, quiz)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        question.update(request.questionText(), request.correctAnswer(), request.explanation(),
                request.score(), request.lessonPage(), request.lessonParagraph());
        question.updateAnchor(resolveAnchor(request.anchorId()));

        if (request.options() != null) {
            List<QuizOption> options = request.options().stream()
                    .map(opt -> QuizOption.builder()
                            .question(question)
                            .optionText(opt.optionText())
                            .correct(opt.correct())
                            .build())
                    .collect(Collectors.toList());
            question.replaceOptions(options);
        }

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

    public boolean isOwner(Long quizId, CustomUserDetails principal) {
        return quizRepository.findById(quizId)
                .map(quiz -> quiz.getProfessor().getId().equals(principal.getUserId()))
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

    private Lesson resolveAnchor(Long anchorId) {
        if (anchorId == null) return null;
        return lessonRepository.findById(anchorId)
                .orElseThrow(() -> new CustomException(ErrorCode.LESSON_NOT_FOUND));
    }
}
