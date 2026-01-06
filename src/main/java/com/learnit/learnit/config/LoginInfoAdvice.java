package com.learnit.learnit.config;

import com.learnit.learnit.cart.CartMapper;
import com.learnit.learnit.cart.GuestCartService;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

//로그인 성공 → 세션에 LOGIN_USER_* 저장
//
//@ControllerAdvice → 세션에서 꺼내서 loginUserId, loginUserName, loginUserRole 모델에 항상 넣어줌
//
//모든 템플릿에서:
//
//${loginUserId} 로 로그인 여부 체크
//
//${loginUserName} 으로 인사 표시
//
//${loginUserRole} 로 권한 따라 메뉴 제어 가능

@ControllerAdvice
@RequiredArgsConstructor
public class LoginInfoAdvice {

    private final CartMapper cartMapper;
    private final GuestCartService guestCartService;

    // 모든 뷰에서 ${loginUserId} 로 사용 가능
    @ModelAttribute("loginUserId")
    public Long loginUserId(HttpSession session) {
        return SessionUtils.getUserId(session);
    }

    // 모든 뷰에서 ${loginUserName} 로 사용 가능
    @ModelAttribute("loginUserName")
    public String loginUserName(HttpSession session) {
        return (String) session.getAttribute("LOGIN_USER_NAME");
    }

    // 모든 뷰에서 ${loginUserRole} 로 사용 가능
    @ModelAttribute("loginUserRole")
    public String loginUserRole(HttpSession session) {
        return (String) session.getAttribute("LOGIN_USER_ROLE");
    }

    // ✅✅ 모든 뷰에서 장바구니 개수 제공 (로그인: DB / 비로그인: 세션)
    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            return guestCartService.getCourseIds(session).size();
        }
        return cartMapper.countByUserId(userId);
    }

}

