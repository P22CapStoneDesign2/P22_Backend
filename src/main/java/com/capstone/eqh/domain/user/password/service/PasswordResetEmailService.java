package com.capstone.eqh.domain.user.password.service;

import com.capstone.eqh.domain.user.password.exception.PasswordResetMailException;
import com.capstone.eqh.global.config.PasswordResetProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 비밀번호 재설정 링크 메일 발송.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;

    @Value("${spring.mail.username:}")
    private String defaultFrom;

    /**
     * @param toEmail 수신 이메일
     * @param token   URL에 포함할 원문 토큰(UUID)
     */
    public void sendPasswordResetLink(String toEmail, String token) {
        String resetLink = buildResetLink(token, toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[안내] 비밀번호 재설정");
        message.setText("""
                비밀번호 재설정을 요청하셨습니다.
                아래 링크를 클릭하여 새 비밀번호를 설정해 주세요.
                (링크는 30분간 유효합니다.)

                %s

                본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                """.formatted(resetLink));

        String from = properties.getMailFrom() != null && !properties.getMailFrom().isBlank()
                ? properties.getMailFrom()
                : defaultFrom;
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }

        try {
            mailSender.send(message);
            log.info("비밀번호 재설정 메일 발송 완료 to={}", toEmail);
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패 to={}", toEmail, e);
            throw new PasswordResetMailException("비밀번호 재설정 메일 발송에 실패했습니다.", e);
        }
    }

    private String buildResetLink(String token, String email) {
        String base = properties.getFrontendResetUrl();
        if (base.contains("?")) {
            return base + "&token=" + token + "&email=" + email;
        }
        return base + "?token=" + token + "&email=" + email;
    }
}
