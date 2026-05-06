package com.capstone.eqh.domain.user.controller;

import com.capstone.eqh.domain.user.dto.request.UserDeleteRequestDto;
import com.capstone.eqh.domain.user.dto.request.UserUpdateRequestDto;
import com.capstone.eqh.domain.user.dto.response.UserResponseDto;
import com.capstone.eqh.domain.user.service.UserService;
import com.capstone.eqh.global.common.ApiResponse;
import com.capstone.eqh.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDto response = userService.getMyProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequestDto request) {
        UserResponseDto response = userService.updateMyProfile(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(200, "회원 정보 수정 성공", response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) UserDeleteRequestDto request) {
        userService.deleteMyAccount(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴 성공"));
    }
}
