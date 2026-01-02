package com.learnit.learnit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Qualifier("gmailMailSender")
    private final JavaMailSender gmailMailSender;

    @Qualifier("naverMailSender")
    private final JavaMailSender naverMailSender;

    @Value("${mail.gmail.username}")
    private String gmailFromEmail;

    @Value("${mail.naver.username}")
    private String naverFromEmail;

    /**
     * 임시 비밀번호 이메일 발송 (Gmail 우선, 실패 시 Naver로 재시도)
     * @param toEmail 수신자 이메일
     * @param tempPassword 임시 비밀번호
     */
    public void sendTempPasswordEmail(String toEmail, String tempPassword) {
        System.out.println("========================================");
        System.out.println("[이메일 발송 시작] 임시 비밀번호 발송");
        System.out.println("수신자: " + toEmail);
        System.out.println("========================================");
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[LearnIT] 임시 비밀번호 발송");
        message.setText(
            "안녕하세요. LearnIT입니다.\n\n" +
            "요청하신 임시 비밀번호를 발송해드립니다.\n\n" +
            "임시 비밀번호: " + tempPassword + "\n\n" +
            "보안을 위해 로그인 후 비밀번호를 변경해주시기 바랍니다.\n\n" +
            "감사합니다."
        );

        // Gmail로 먼저 시도
        try {
            message.setFrom("LearnIT <" + gmailFromEmail + ">");
            System.out.println("[이메일 발송] Gmail로 전송 시도 중...");
            gmailMailSender.send(message);
            System.out.println("[이메일 발송 성공] Gmail로 임시 비밀번호 이메일이 성공적으로 발송되었습니다.");
            System.out.println("========================================");
            return;
        } catch (Exception e) {
            System.out.println("[이메일 발송 실패] Gmail 발송 중 오류 발생: " + e.getMessage());
            System.out.println("[이메일 발송] Naver로 재시도합니다...");
        }

        // Naver로 재시도
        try {
            message.setFrom("LearnIT <" + naverFromEmail + ">");
            naverMailSender.send(message);
            System.out.println("[이메일 발송 성공] Naver로 임시 비밀번호 이메일이 성공적으로 발송되었습니다.");
            System.out.println("========================================");
        } catch (Exception e) {
            System.out.println("[이메일 발송 실패] Naver 발송 중 오류 발생: " + e.getMessage());
            System.out.println("오류 타입: " + e.getClass().getName());
            e.printStackTrace();
            System.out.println("========================================");
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다. (Gmail과 Naver 모두 실패)", e);
        }
    }

    /**
     * 비밀번호 변경 알림 이메일 발송 (Gmail 우선, 실패 시 Naver로 재시도)
     * @param toEmail 수신자 이메일
     * @param userName 사용자 이름
     */
    public void sendPasswordChangedEmail(String toEmail, String userName) {
        System.out.println("========================================");
        System.out.println("[이메일 발송 시작] 비밀번호 변경 알림");
        System.out.println("수신자: " + toEmail);
        System.out.println("사용자 이름: " + userName);
        System.out.println("========================================");
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[LearnIT] 비밀번호 변경 알림");
        message.setText(
            "안녕하세요. " + (userName != null ? userName : "고객") + "님, LearnIT입니다.\n\n" +
            "귀하의 계정 비밀번호가 성공적으로 변경되었습니다.\n\n" +
            "만약 본인이 비밀번호를 변경하지 않으셨다면, 즉시 고객센터로 문의해주시기 바랍니다.\n\n" +
            "감사합니다."
        );

        // Gmail로 먼저 시도
        try {
            message.setFrom("LearnIT <" + gmailFromEmail + ">");
            System.out.println("[이메일 발송] Gmail로 전송 시도 중...");
            gmailMailSender.send(message);
            System.out.println("[이메일 발송 성공] Gmail로 비밀번호 변경 알림 이메일이 성공적으로 발송되었습니다.");
            System.out.println("========================================");
            return;
        } catch (Exception e) {
            System.out.println("[이메일 발송 실패] Gmail 발송 중 오류 발생: " + e.getMessage());
            System.out.println("[이메일 발송] Naver로 재시도합니다...");
        }

        // Naver로 재시도
        try {
            message.setFrom("LearnIT <" + naverFromEmail + ">");
            naverMailSender.send(message);
            System.out.println("[이메일 발송 성공] Naver로 비밀번호 변경 알림 이메일이 성공적으로 발송되었습니다.");
            System.out.println("========================================");
        } catch (Exception e) {
            System.out.println("[이메일 발송 실패] Naver 발송 중 오류 발생: " + e.getMessage());
            System.out.println("오류 타입: " + e.getClass().getName());
            e.printStackTrace();
            System.out.println("========================================");
            // 이메일 발송 실패는 로깅만 하고 예외를 던지지 않음 (비밀번호 변경은 이미 완료되었으므로)
            System.err.println("비밀번호 변경 알림 이메일 발송 실패 (Gmail과 Naver 모두 실패): " + e.getMessage());
        }
    }

    /**
     * 회원가입 완료 환영 이메일 발송 (Gmail 우선, 실패 시 Naver로 재시도)
     * @param toEmail 수신자 이메일
     * @param userName 사용자 이름
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        System.out.println("========================================");
        System.out.println("[이메일 발송 시작] 회원가입 완료 환영 이메일");
        System.out.println("수신자: " + toEmail);
        System.out.println("사용자 이름: " + userName);
        System.out.println("========================================");
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[LearnIT] 회원가입을 환영합니다!");
        message.setText(
            "안녕하세요. " + (userName != null ? userName : "고객") + "님, LearnIT입니다.\n\n" +
            "회원가입이 완료되었습니다. LearnIT와 함께 성장하는 시간이 되기를 바랍니다.\n\n" +
            "다양한 강의와 학습 자료를 통해 지식을 쌓아가시기 바랍니다.\n\n" +
            "감사합니다.\n\n" +
            "LearnIT 팀"
        );

        // Gmail로 먼저 시도
        try {
            message.setFrom("LearnIT <" + gmailFromEmail + ">");
            System.out.println("[이메일 발송] Gmail로 전송 시도 중...");
            gmailMailSender.send(message);
            System.out.println("[이메일 발송 성공] Gmail로 회원가입 완료 환영 이메일이 성공적으로 발송되었습니다.");
            System.out.println("========================================");
            return;
        } catch (Exception e) {
            System.out.println("[이메일 발송 실패] Gmail 발송 중 오류 발생: " + e.getMessage());
            System.out.println("[이메일 발송] Naver로 재시도합니다...");
        }

        // Naver로 재시도
        try {
            message.setFrom("LearnIT <" + naverFromEmail + ">");
            naverMailSender.send(message);
            System.out.println("[이메일 발송 성공] Naver로 회원가입 완료 환영 이메일이 성공적으로 발송되었습니다.");
            System.out.println("========================================");
        } catch (Exception e) {
            System.out.println("[이메일 발송 실패] Naver 발송 중 오류 발생: " + e.getMessage());
            System.out.println("오류 타입: " + e.getClass().getName());
            e.printStackTrace();
            System.out.println("========================================");
            // 이메일 발송 실패는 로깅만 하고 예외를 던지지 않음 (회원가입은 이미 완료되었으므로)
            System.err.println("회원가입 완료 이메일 발송 실패 (Gmail과 Naver 모두 실패): " + e.getMessage());
        }
    }
}

