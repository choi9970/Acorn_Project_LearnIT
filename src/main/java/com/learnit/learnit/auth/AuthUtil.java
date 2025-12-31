package com.learnit.learnit.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AuthUtil {

    public static Long getLoginUserId() {
        // 1) Spring Security 기준 (OAuth2 principal 등)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();

            if (principal instanceof CustomOAuth2User custom) {
                return custom.getUserId();
            }
            if (principal instanceof LoginUserIdProvider provider) {
                return provider.getUserId();
            }
            // principal이 "anonymousUser" 같은 경우는 계속 진행
        }

        // 2) ✅ 세션 기준 (local 로그인)
        try {
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(false);
            if (session == null) return null;

            Object id = session.getAttribute("LOGIN_USER_ID");
            if (id instanceof Long l) return l;
            if (id instanceof Integer i) return i.longValue();

        } catch (Exception ignore) {}

        return null;
    }

    public static Long requireLoginUserId() {
        return getLoginUserId();
    }
}
