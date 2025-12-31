package com.learnit.learnit.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails, LoginUserIdProvider {

    private final Long userId;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserDetails(Long userId,
                             String email,
                             String password,
                             Collection<? extends GrantedAuthority> authorities,
                             boolean enabled) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public String getUsername() { // Spring Security에서 username 역할 = email
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
