package com.learnit.learnit.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // 오류 메시지를 세션에 저장하여 로그인 페이지에서 표시
        String errorMessage = "소셜 로그인에 실패했습니다.";
        if (exception.getMessage() != null && !exception.getMessage().isEmpty()) {
            errorMessage += " (" + exception.getMessage() + ")";
        }
        request.getSession().setAttribute("oauthError", errorMessage);
        
        response.sendRedirect("/login?error=true");
    }
}

