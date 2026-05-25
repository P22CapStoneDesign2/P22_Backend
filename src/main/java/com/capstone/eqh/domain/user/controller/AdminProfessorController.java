package com.capstone.eqh.domain.user.controller;

import com.capstone.eqh.domain.user.dto.request.ProfessorStatusUpdateRequestDto;
import com.capstone.eqh.domain.user.dto.response.PendingProfessorResponseDto;
import com.capstone.eqh.domain.user.dto.response.ProfessorStatusResponseDto;
import com.capstone.eqh.domain.user.service.AdminProfessorService;
import com.capstone.eqh.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/professors")
@RequiredArgsConstructor
public class AdminProfessorController {

    private final AdminProfessorService adminProfessorService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<PendingProfessorResponseDto>>> listPendingProfessors(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "승인 대기 목록 조회 성공",
                adminProfessorService.listPendingProfessors(pageable)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProfessorStatusResponseDto>> approveProfessor(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "계정을 승인했습니다.",
                adminProfessorService.approveProfessor(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProfessorStatusResponseDto>> rejectProfessor(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "계정을 거절했습니다.",
                adminProfessorService.rejectProfessor(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProfessorStatusResponseDto>> changeProfessorStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProfessorStatusUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(200, "계정 상태를 변경했습니다.",
                adminProfessorService.changeProfessorStatus(id, request.status())));
    }
}
