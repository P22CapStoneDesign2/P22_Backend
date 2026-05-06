package com.capstone.eqh.domain.user.controller;

import com.capstone.eqh.domain.user.dto.request.LoginRequestDto;
import com.capstone.eqh.domain.user.dto.request.LogoutRequestDto;
import com.capstone.eqh.domain.user.dto.request.ReissueRequestDto;
import com.capstone.eqh.domain.user.dto.request.SignupRequestDto;
import com.capstone.eqh.domain.user.dto.response.AuthResponseDto;
import com.capstone.eqh.domain.user.service.UserAuthService;
import com.capstone.eqh.domain.user.service.UserSignupService;
import com.capstone.eqh.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserSignupService userSignupService;
    private final UserAuthService userAuthService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequestDto request) {
        userSignupService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "회원가입 성공", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = userAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success(200, "로그인 성공", response));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthResponseDto>> reissue(@Valid @RequestBody ReissueRequestDto request) {
        AuthResponseDto response = userAuthService.reissue(request);
        return ResponseEntity.ok(ApiResponse.success(200, "토큰 재발급 성공", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequestDto request) {
        userAuthService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }
}
