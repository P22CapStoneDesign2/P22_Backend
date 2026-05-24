package com.capstone.eqh.domain.user.controller;

import com.capstone.eqh.domain.user.dto.request.UserStatusUpdateRequestDto;
import com.capstone.eqh.domain.user.dto.response.PendingUserResponseDto;
import com.capstone.eqh.domain.user.dto.response.UserStatusResponseDto;
import com.capstone.eqh.domain.user.service.AdminUserService;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<PendingUserResponseDto>>> listPending(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "승인 대기 목록 조회 성공",
                adminUserService.listPending(pageable)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<UserStatusResponseDto>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "계정을 승인했습니다.",
                adminUserService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<UserStatusResponseDto>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "계정을 거절했습니다.",
                adminUserService.reject(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserStatusResponseDto>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(200, "계정 상태를 변경했습니다.",
                adminUserService.changeStatus(id, request.status())));
    }
}
