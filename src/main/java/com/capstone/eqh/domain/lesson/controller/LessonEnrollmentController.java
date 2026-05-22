package com.capstone.eqh.domain.lesson.controller;

import com.capstone.eqh.domain.lesson.dto.response.EnrollmentDecisionResponseDto;
import com.capstone.eqh.domain.lesson.dto.response.EnrollmentListItemResponseDto;
import com.capstone.eqh.domain.lesson.dto.response.EnrollmentResponseDto;
import com.capstone.eqh.domain.lesson.dto.response.MyLessonResponseDto;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.lesson.service.LessonEnrollmentService;
import com.capstone.eqh.global.common.ApiResponse;
import com.capstone.eqh.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonEnrollmentController {

    private final LessonEnrollmentService enrollmentService;

    // ── 학생용 ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/enrollments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<EnrollmentResponseDto>> request(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        EnrollmentResponseDto response = enrollmentService.request(id, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "수강 신청이 접수되었습니다.", response));
    }

    @DeleteMapping("/{id}/enrollments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        enrollmentService.cancel(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("수강 신청이 취소되었습니다."));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<MyLessonResponseDto>>> listMyApproved(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "decidedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MyLessonResponseDto> page = enrollmentService.listMyApproved(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(200, "승인 교안 목록 조회 성공", page));
    }

    // ── 교수/관리자용 ────────────────────────────────────────────────────

    @GetMapping("/{id}/enrollments")
    @PreAuthorize("(hasRole('PROF') and @lessonService.isOwner(#id, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EnrollmentListItemResponseDto>>> listByLesson(
            @PathVariable Long id,
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 10, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<EnrollmentListItemResponseDto> page = enrollmentService.listByLesson(id, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(200, "수강 신청 목록 조회 성공", page));
    }

    @PostMapping("/{id}/enrollments/{enrollmentId}/approve")
    @PreAuthorize("(hasRole('PROF') and @lessonService.isOwner(#id, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentDecisionResponseDto>> approve(
            @PathVariable Long id,
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        EnrollmentDecisionResponseDto response = enrollmentService.approve(id, enrollmentId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(200, "수강 신청을 수락했습니다.", response));
    }

    @PostMapping("/{id}/enrollments/{enrollmentId}/reject")
    @PreAuthorize("(hasRole('PROF') and @lessonService.isOwner(#id, principal.userId)) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentDecisionResponseDto>> reject(
            @PathVariable Long id,
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        EnrollmentDecisionResponseDto response = enrollmentService.reject(id, enrollmentId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(200, "수강 신청을 거절했습니다.", response));
    }
}
