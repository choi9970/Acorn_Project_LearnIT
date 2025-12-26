package com.learnit.learnit.config;

import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.mapper.UserMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserMapper userMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        // OAuthService에서 이미 세션에 사용자 정보를 저장했으므로 세션에서 조회
        Long userId = (Long) request.getSession().getAttribute("LOGIN_USER_ID");
        
        if (userId != null) {
            User user = userMapper.selectUserEntityById(userId);
            
            // SIGNUP_PENDING 상태면 추가 정보 입력 페이지로 리다이렉트
            if (user != null && User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
                getRedirectStrategy().sendRedirect(request, response, "/user/additional-info");
                return;
            }
        }
        
        // 추가 정보 완료 시 홈으로 리다이렉트
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

