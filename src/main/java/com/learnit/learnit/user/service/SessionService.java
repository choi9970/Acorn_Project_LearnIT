package com.learnit.learnit.user.service;

import com.learnit.learnit.user.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 세션 관리 서비스
 * 로그인 정보를 세션에 저장하는 로직을 통합
 */
@Service
public class SessionService {

    /**
     * 세션에 로그인 정보 저장
     * @param session HttpSession
     * @param user 사용자 정보
     */
    public void setLoginSession(HttpSession session, User user) {
        if (session == null || user == null) {
            return;
        }
        
        session.setAttribute("LOGIN_USER_ID", user.getUserId());
        session.setAttribute("LOGIN_USER_NAME", user.getName());
        session.setAttribute("LOGIN_USER_ROLE", user.getRole());
    }

    /**
     * RequestContextHolder를 통해 세션에 로그인 정보 저장
     * OAuth 인증 플로우에서 사용 (RequestContextHolder가 사용 가능한 경우)
     * @param user 사용자 정보
     * @return 세션 저장 성공 여부
     */
    public boolean setLoginSessionFromContext(User user) {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession();
            setLoginSession(session, user);
            return true;
        } catch (IllegalStateException e) {
            // RequestContextHolder가 사용 불가능한 경우 (OAuth 인증 플로우)
            return false;
        }
    }
}

