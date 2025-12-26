package com.learnit.learnit.user.interceptor;

import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 인증 인터셉터
 * 세션에서 로그인 정보를 확인하고 필요시 리다이렉트 처리
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        
        // 세션이 없거나 로그인 정보가 없으면 통과 (Spring Security가 처리)
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            return true;
        }
        
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        User user = userMapper.selectUserEntityById(userId);
        
        if (user == null) {
            return true;
        }
        
        String requestURI = request.getRequestURI();
        
        // SIGNUP_PENDING 상태 사용자는 추가 정보 입력 페이지로만 접근 가능
        if (User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
            // 추가 정보 입력 페이지, 정적 리소스, 로그인/로그아웃 페이지는 제외
            if (!requestURI.equals("/user/additional-info") 
                && !requestURI.startsWith("/css/")
                && !requestURI.startsWith("/js/")
                && !requestURI.startsWith("/images/")
                && !requestURI.startsWith("/files/")
                && !requestURI.equals("/logout")
                && !requestURI.startsWith("/oauth2/")
                && !requestURI.equals("/login")
                && !requestURI.equals("/signup")
                && !requestURI.equals("/home")
                && !requestURI.equals("/")) {
                response.sendRedirect("/user/additional-info");
                return false;
            }
        }
        
        // 마이페이지는 ACTIVE 상태만 접근 가능
        if (requestURI.startsWith("/mypage")) {
            if (!User.STATUS_ACTIVE.equals(user.getStatus())) {
                if (User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
                    response.sendRedirect("/user/additional-info");
                } else {
                    response.sendRedirect("/login");
                }
                return false;
            }
        }
        
        return true;
    }
}

