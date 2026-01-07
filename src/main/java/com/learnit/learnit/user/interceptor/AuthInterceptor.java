package com.learnit.learnit.user.interceptor;

import com.learnit.learnit.user.util.SessionUtils;
import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.repository.UserRepository;
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

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String requestURI = request.getRequestURI();
        
        // API 경로는 인터셉터에서 제외 (컨트롤러에서 직접 처리)
        if (requestURI.startsWith("/api/")) {
            return true;
        }
        
        // 관리자 페이지 접근 제어 (가장 먼저 체크)
        if (requestURI.startsWith("/admin")) {
            // 로그인하지 않은 경우
            if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
                response.sendRedirect("/login");
                return false;
            }
            
            // 관리자 권한 체크
            String role = (String) session.getAttribute("LOGIN_USER_ROLE");
            if (role == null || !"ADMIN".equals(role.trim())) {
                response.sendRedirect("/home?error=unauthorized");
                return false;
            }
        }
        
        // 세션이 없거나 로그인 정보가 없으면 통과 (Spring Security가 처리)
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            return true;
        }
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            return true;
        }
        
        // SIGNUP_PENDING 상태 사용자는 추가 정보 입력 페이지로만 접근 가능
        // 단, 관리자는 관리자 페이지 접근 허용
        if (User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
            // 관리자 페이지는 SIGNUP_PENDING 상태여도 접근 허용
            if (requestURI.startsWith("/admin")) {
                return true;
            }
            
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
        
        // 마이페이지 접근 제어
        if (requestURI.startsWith("/mypage")) {
            // 일반 로그인 사용자(provider가 null 또는 "local")는 마이페이지 접근 허용
            // 소셜 로그인 사용자(google, kakao)만 status 체크
            String provider = user.getProvider();
            boolean isSocialLogin = provider != null && 
                                   !provider.equals("local") && 
                                   (provider.equals("google") || provider.equals("kakao"));
            
            if (isSocialLogin) {
                // 소셜 로그인 사용자는 ACTIVE 상태만 접근 가능
                if (!User.STATUS_ACTIVE.equals(user.getStatus())) {
                    if (User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
                        response.sendRedirect("/user/additional-info");
                    } else {
                        response.sendRedirect("/login");
                    }
                    return false;
                }
            }
            // 일반 로그인 사용자(provider가 null 또는 "local")는 status와 관계없이 접근 허용
        }
        
        return true;
    }
}

