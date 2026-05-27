package com.capstone.eqh.domain.user.password.controller;

import com.capstone.eqh.domain.user.password.dto.request.PasswordResetConfirmRequestDto;
import com.capstone.eqh.domain.user.password.dto.request.PasswordResetRequestDto;
import com.capstone.eqh.domain.user.password.dto.response.PasswordResetResponse;
import com.capstone.eqh.domain.user.password.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 비밀번호 재설정 API 컨트롤러 (명세: password-reset-api-spec.md).
 * <p>
 * JWT 인증 없이 호출 가능하며, 모든 응답은 {@code { success, message }} 형식이다.
 * 실제 비즈니스 로직은 {@link PasswordResetService}에서 처리한다.
 */
@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * [1단계] 이메일 유효성 검증
     * <p>
     * 비밀번호 재설정 화면에서 사용자가 이메일을 입력한 뒤 호출한다.
     * 가입된 LOCAL(일반) 계정인지 확인하고, 재설정 가능 여부를 알려 준다.
     * <ul>
     *   <li>성공: {@code success: true} — 재설정 메일 요청 가능</li>
     *   <li>실패: 미가입 이메일, 소셜(카카오) 가입 계정 등</li>
     * </ul>
     * POST /api/v1/auth/password/validate-email
     */
    @PostMapping("/validate-email")
    public ResponseEntity<PasswordResetResponse> validateEmail(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        return ResponseEntity.ok(passwordResetService.validateEmailEligibility(request.email()));
    }

    /**
     * [2단계] 비밀번호 재설정 메일 발송
     * <p>
     * 이메일 검증을 통과한 뒤, 사용자가 "재설정 메일 보내기" 등을 누르면 호출한다.
     * UUID 토큰을 생성·DB·Redis에 저장한 뒤, 재설정 링크가 담긴 이메일을 발송한다.
     * 동일 이메일은 1분 간격으로만 재요청할 수 있다.
     * POST /api/v1/auth/password/reset-request
     */
    @PostMapping("/reset-request")
    public ResponseEntity<PasswordResetResponse> requestReset(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        PasswordResetResponse body = passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(body);
    }

    /**
     * [3단계] 재설정 토큰 검증
     * <p>
     * 사용자가 이메일의 재설정 링크를 클릭해 프론트 화면에 진입할 때 호출한다.
     * URL 쿼리 파라미터({@code ?token=...})의 토큰이 유효·미만료·미사용인지 확인한다.
     * <ul>
     *   <li>성공: {@code "유효한 토큰입니다."}</li>
     *   <li>실패: 만료·유효하지 않은 토큰</li>
     * </ul>
     * GET /api/v1/auth/password/verify-token?token=
     */
    @GetMapping("/verify-token")
    public ResponseEntity<PasswordResetResponse> verifyToken(
            @RequestParam("token") String token
    ) {
        PasswordResetResponse body = passwordResetService.verifyToken(token);
        return ResponseEntity.ok(body);
    }

    /**
     * [4단계] 새 비밀번호 저장
     * <p>
     * 토큰 검증을 통과한 화면에서 사용자가 새 비밀번호와 확인 비밀번호를 입력 후 제출할 때 호출한다.
     * BCrypt로 암호화해 저장하고, 해당 토큰은 사용 처리(1회용)한다.
     * POST /api/v1/auth/password/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequestDto request
    ) {
        PasswordResetResponse body = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(body);
    }
}
