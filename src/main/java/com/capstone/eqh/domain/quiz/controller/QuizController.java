package com.capstone.eqh.domain.quiz.controller;

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
import com.capstone.eqh.domain.quiz.service.QuizService;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.global.common.ApiResponse;
import com.capstone.eqh.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // ── 퀴즈 세트 CRUD ──────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('PROF') and principal.active")
    public ResponseEntity<ApiResponse<QuizDetailResponseDto>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody QuizCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "퀴즈 생성 성공",
                        quizService.create(request, userDetails.getUserId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<QuizResponseDto>>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long materialId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Role role = userDetails.getUser().getRole();
        return ResponseEntity.ok(ApiResponse.success(200, "퀴즈 목록 조회 성공",
                quizService.getAll(userDetails.getUserId(), role, materialId, pageable)));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizDetailResponseDto>> getOne(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Role role = userDetails.getUser().getRole();
        return ResponseEntity.ok(ApiResponse.success(200, "퀴즈 조회 성공",
                quizService.getOne(quizId, userDetails.getUserId(), role)));
    }

    @GetMapping("/{quizId}/edit")
    @PreAuthorize("(hasRole('PROF') and principal.active and @quizService.isOwner(#quizId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuizEditResponseDto>> getForEdit(@PathVariable Long quizId) {
        return ResponseEntity.ok(ApiResponse.success(200, "퀴즈 수정용 조회 성공",
                quizService.getForEdit(quizId)));
    }

    @PutMapping("/{quizId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @quizService.isOwner(#quizId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponseDto>> update(
            @PathVariable Long quizId,
            @Valid @RequestBody QuizUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(200, "퀴즈 수정 성공",
                quizService.update(quizId, request)));
    }

    @DeleteMapping("/{quizId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @quizService.isOwner(#quizId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long quizId) {
        quizService.delete(quizId);
        return ResponseEntity.ok(ApiResponse.success("퀴즈 삭제 성공"));
    }

    // ── 문제 관리 ────────────────────────────────────────────────────────

    @PostMapping("/{quizId}/questions")
    @PreAuthorize("(hasRole('PROF') and principal.active and @quizService.isOwner(#quizId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuizQuestionResponseDto>> addQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody QuizQuestionCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "문제 추가 성공",
                        quizService.addQuestion(quizId, request)));
    }

    @PutMapping("/{quizId}/questions/{questionId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @quizService.isOwner(#quizId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuizQuestionResponseDto>> updateQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuizQuestionUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(200, "문제 수정 성공",
                quizService.updateQuestion(quizId, questionId, request)));
    }

    @DeleteMapping("/{quizId}/questions/{questionId}")
    @PreAuthorize("(hasRole('PROF') and principal.active and @quizService.isOwner(#quizId, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId) {
        quizService.deleteQuestion(quizId, questionId);
        return ResponseEntity.ok(ApiResponse.success("문제 삭제 성공"));
    }

    // ── 학생 답안 제출 및 오답 조회 ─────────────────────────────────────

    @PostMapping("/{quizId}/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<QuizSubmissionResponseDto>> submit(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody QuizSubmitRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(200, "퀴즈 제출 성공",
                quizService.submit(quizId, request, userDetails.getUserId())));
    }

    @GetMapping("/wrong-answers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<WrongAnswerResponseDto>>> getWrongAnswers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "submission.submittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "오답 목록 조회 성공",
                quizService.getWrongAnswers(userDetails.getUserId(), pageable)));
    }
}
