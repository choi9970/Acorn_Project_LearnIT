package com.learnit.learnit.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * OAuth2 로그인 principal에 userId를 추가로 담기 위한 래퍼 클래스
 */
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate; // 원래 OAuth2User
    private final Long userId;         // DB user_id

    public CustomOAuth2User(OAuth2User delegate, Long userId) {
        this.delegate = delegate;
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    // ✅ 필수 구현 1) 권한
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    // ✅ 필수 구현 2) OAuth2 attributes
    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    // ✅ 필수 구현 3) name (provider가 주는 고유키)
    @Override
    public String getName() {
        return delegate.getName();
    }
}
