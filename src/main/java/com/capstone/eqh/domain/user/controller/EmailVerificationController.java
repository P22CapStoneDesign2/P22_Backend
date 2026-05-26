package com.capstone.eqh.domain.user.controller;

import com.capstone.eqh.domain.user.dto.request.EmailSendRequestDto;
import com.capstone.eqh.domain.user.dto.request.EmailVerifyRequestDto;
import com.capstone.eqh.domain.user.service.EmailVerificationService;
import com.capstone.eqh.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody EmailSendRequestDto request) {
        emailVerificationService.sendVerificationCode(request.email());
        return ResponseEntity.ok(ApiResponse.success("인증번호가 전송되었습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody EmailVerifyRequestDto request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));
    }
}
