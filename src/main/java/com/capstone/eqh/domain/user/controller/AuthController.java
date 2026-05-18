package com.capstone.eqh.domain.user.controller;

import com.capstone.eqh.domain.user.dto.request.LoginRequestDto;
import com.capstone.eqh.domain.user.dto.request.LogoutRequestDto;
import com.capstone.eqh.domain.user.dto.request.ProfSignupRequestDto;
import com.capstone.eqh.domain.user.dto.request.ReissueRequestDto;
import com.capstone.eqh.domain.user.dto.request.UserSocialSignupRequestDto;
import com.capstone.eqh.domain.user.dto.response.AuthResponseDto;
import com.capstone.eqh.domain.user.dto.response.NicknameCheckResponseDto;
import com.capstone.eqh.domain.user.service.UserAuthService;
import com.capstone.eqh.domain.user.service.UserSignupService;
import com.capstone.eqh.global.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserSignupService userSignupService;
    private final UserAuthService userAuthService;

    @PostMapping("/profsignup")
    public ResponseEntity<ApiResponse<Void>> profSignup(@Valid @RequestBody ProfSignupRequestDto request) {
        userSignupService.profSignup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "회원가입 성공", null));
    }

    @PostMapping("/usersignup")
    public ResponseEntity<ApiResponse<AuthResponseDto>> userSignup(@Valid @RequestBody UserSocialSignupRequestDto request) {
        AuthResponseDto response = userAuthService.completeSocialSignup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "회원가입 성공", response));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponseDto>> checkNickname(
            @RequestParam @NotBlank(message = "닉네임은 필수입니다.") String nickname) {
        boolean available = userSignupService.isNicknameAvailable(nickname);
        String message = available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.";
        return ResponseEntity.ok(ApiResponse.success(200, message, new NicknameCheckResponseDto(available)));
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
