package com.learnit.learnit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${mail.gmail.host}")
    private String gmailHost;

    @Value("${mail.gmail.port}")
    private int gmailPort;

    @Value("${mail.gmail.username}")
    private String gmailUsername;

    @Value("${mail.gmail.password}")
    private String gmailPassword;

    @Value("${mail.naver.host}")
    private String naverHost;

    @Value("${mail.naver.port}")
    private int naverPort;

    @Value("${mail.naver.username}")
    private String naverUsername;

    @Value("${mail.naver.password}")
    private String naverPassword;

    /**
     * Gmail JavaMailSender (기본/주 사용)
     */
    @Primary
    @Bean(name = "gmailMailSender")
    public JavaMailSender gmailMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(gmailHost);
        mailSender.setPort(gmailPort);
        mailSender.setUsername(gmailUsername);
        mailSender.setPassword(gmailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Naver JavaMailSender (Fallback용)
     */
    @Bean(name = "naverMailSender")
    public JavaMailSender naverMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(naverHost);
        mailSender.setPort(naverPort);
        mailSender.setUsername(naverUsername);
        mailSender.setPassword(naverPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", naverHost);
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }
}

