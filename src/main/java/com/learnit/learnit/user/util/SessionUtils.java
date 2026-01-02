package com.learnit.learnit.user.util;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 세션 관련 유틸리티 클래스
 * 로그인 사용자 ID를 세션에서 가져오는 기능 제공
 */
public class SessionUtils {
    
    private static final String LOGIN_USER_ID = "LOGIN_USER_ID";
    
    /**
     * RequestContextHolder를 통해 세션에서 사용자 ID를 가져옵니다.
     * 
     * @return 사용자 ID, 로그인하지 않은 경우 null
     */
    public static Long getLoginUserId() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(false);
            if (session == null) {
                return null;
            }
            Object id = session.getAttribute(LOGIN_USER_ID);
            if (id instanceof Long l) return l;
            if (id instanceof Integer i) return i.longValue();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 세션에서 사용자 ID를 가져옵니다.
     * 
     * @param session HttpSession
     * @return 사용자 ID, 로그인하지 않은 경우 null
     */
    public static Long getUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(LOGIN_USER_ID);
        if (id instanceof Long l) return l;
        if (id instanceof Integer i) return i.longValue();
        return null;
    }
    
    /**
     * 로그인 여부를 확인합니다.
     * 
     * @param session HttpSession
     * @return 로그인한 경우 true, 그렇지 않으면 false
     */
    public static boolean isLoggedIn(HttpSession session) {
        return getUserId(session) != null;
    }
    
    /**
     * RequestContextHolder를 통해 세션에서 사용자 ID를 가져오고, 없으면 예외를 발생시킵니다.
     * 
     * @return 사용자 ID
     * @throws IllegalStateException 로그인하지 않은 경우
     */
    public static Long requireLoginUserId() {
        Long userId = getLoginUserId();
        if (userId == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return userId;
    }
    
    /**
     * 세션에서 사용자 ID를 가져오고, 없으면 예외를 발생시킵니다.
     * 
     * @param session HttpSession
     * @return 사용자 ID
     * @throws IllegalStateException 로그인하지 않은 경우
     */
    public static Long requireUserId(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return userId;
    }
}

