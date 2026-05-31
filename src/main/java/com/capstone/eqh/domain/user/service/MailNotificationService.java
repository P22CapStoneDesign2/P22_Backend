package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.global.config.EmailVerificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailNotificationService {

    private final JavaMailSender mailSender;
    private final EmailVerificationProperties verificationProperties;

    public void sendVerificationCode(String toEmail, String plainCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[인증] 이메일 인증번호 안내");
        int minutes = Math.max(1, verificationProperties.getCodeTtlSeconds() / 60);
        message.setText("인증번호는 " + plainCode + " 입니다. " + minutes + "분간 유효합니다.");
        mailSender.send(message);
        log.info("이메일 인증번호 발송 완료 to={}", toEmail);
    }
}
