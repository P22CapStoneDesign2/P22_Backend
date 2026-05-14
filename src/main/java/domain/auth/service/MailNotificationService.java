package domain.auth.service;

import global.config.EmailVerificationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * JavaMailSender 기반 이메일 발송 구현.
 * 실제 SMTP 설정은 application.yml의 spring.mail.* 에 맞춘다.
 */
@Service
public class MailNotificationService {

    private final JavaMailSender mailSender;
    private final EmailVerificationProperties verificationProperties;

    public MailNotificationService(JavaMailSender mailSender, EmailVerificationProperties verificationProperties) {
        this.mailSender = mailSender;
        this.verificationProperties = verificationProperties;
    }

    /**
     * 6자리 인증번호를 메일 본문으로 전송한다.
     * 운영 환경에서는 템플릿(Thymeleaf 등)으로 교체하는 것이 좋다.
     */
    public void sendVerificationCode(String toEmail, String plainCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[인증] 이메일 인증번호 안내");
        int minutes = Math.max(1, verificationProperties.getCodeTtlSeconds() / 60);
        message.setText("인증번호는 " + plainCode + " 입니다. " + minutes + "분간 유효합니다.");
        //mailSender.send(message);
        System.out.println("인증번호 전송 테스트"); 
        System.out.println("서비스 진입 성공");
        System.out.println("=== 메일 전송 MOCK 테스트 ===");
        System.out.println("수신 이메일: " + toEmail);
        System.out.println("인증번호: " + plainCode);
        System.out.println("=== 메일 전송 MOCK 테스트 완료 ===");
    }
}
