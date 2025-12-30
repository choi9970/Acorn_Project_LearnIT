package com.learnit.learnit.mypage.util;

/**
 * 프로필 유효성 검증 유틸리티 클래스
 */
public class ProfileValidator {
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String PHONE_PATTERN = "^[0-9-]+$";
    
    /**
     * 비밀번호 유효성 검증
     * 
     * @param password 비밀번호
     * @throws IllegalArgumentException 유효하지 않은 비밀번호인 경우
     */
    public static void validatePassword(String password) {
        if (password != null && !password.trim().isEmpty()) {
            String trimmedPassword = password.trim();
            if (trimmedPassword.length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("비밀번호는 최소 " + MIN_PASSWORD_LENGTH + "자 이상이어야 합니다.");
            }
        }
    }
    
    /**
     * 이메일 유효성 검증
     * 
     * @param email 이메일
     * @throws IllegalArgumentException 유효하지 않은 이메일인 경우
     */
    public static void validateEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            String trimmedEmail = email.trim();
            if (!trimmedEmail.matches(EMAIL_PATTERN)) {
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
            }
        }
    }
    
    /**
     * 전화번호 유효성 검증
     * 
     * @param phone 전화번호
     * @throws IllegalArgumentException 유효하지 않은 전화번호인 경우
     */
    public static void validatePhone(String phone) {
        if (phone != null && !phone.trim().isEmpty()) {
            String trimmedPhone = phone.trim();
            if (!trimmedPhone.matches(PHONE_PATTERN)) {
                throw new IllegalArgumentException("올바른 전화번호 형식이 아닙니다.");
            }
        }
    }
}

