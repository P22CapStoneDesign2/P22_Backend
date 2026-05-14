package domain.auth.controller;

import domain.auth.dto.request.EmailSendRequest;
import domain.auth.dto.request.VerifyEmailRequest;
import domain.auth.service.EmailVerificationService;
import global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 이메일 인증 REST 컨트롤러.
 */
@RestController
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationController.class);

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * 인증번호 발송 API.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody EmailSendRequest request) {
        System.out.println("[TRACE] 1. Controller.send 진입");
        System.out.println("[TRACE] 1-1. request.email()=" + request.email());
        log.info("[TRACE] 1. Controller.send 진입 email={}", request.email());

        ApiResponse<Void> body = emailVerificationService.sendVerificationCode(request.email());

        System.out.println("[TRACE] 4. Controller.send 서비스 반환 직전 success=" + body.isSuccess());
        log.info("[TRACE] 4. Controller.send 응답 조립 완료");
        return ResponseEntity.ok(body);
    }

    /**
     * 인증번호 검증 API.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyEmailRequest request) {
        System.out.println("[TRACE] 1. Controller.verify 진입");
        System.out.println("[TRACE] 1-1. request.email()=" + request.email());
        log.info("[TRACE] 1. Controller.verify 진입 email={}", request.email());

        emailVerificationService.verifyCode(request.email(), request.code());

        System.out.println("[TRACE] 7. Controller.verify 서비스 성공, 200 응답");
        log.info("[TRACE] 7. Controller.verify 응답 조립 완료");
        return ResponseEntity.ok(ApiResponse.ok("이메일 인증이 완료되었습니다."));
    }
}
