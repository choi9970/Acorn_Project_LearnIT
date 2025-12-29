package com.learnit.learnit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 임시 비밀번호 이메일 발송
     * @param toEmail 수신자 이메일
     * @param tempPassword 임시 비밀번호
     */
    public void sendTempPasswordEmail(String toEmail, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LearnIT] 임시 비밀번호 발송");
            message.setText(
                "안녕하세요. LearnIT입니다.\n\n" +
                "요청하신 임시 비밀번호를 발송해드립니다.\n\n" +
                "임시 비밀번호: " + tempPassword + "\n\n" +
                "보안을 위해 로그인 후 비밀번호를 변경해주시기 바랍니다.\n\n" +
                "감사합니다."
            );

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }
}

