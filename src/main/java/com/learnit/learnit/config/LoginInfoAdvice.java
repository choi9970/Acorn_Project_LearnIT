package com.learnit.learnit.config;

import jakarta.servlet.http.HttpSession;
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
public class LoginInfoAdvice {

    // 모든 뷰에서 ${loginUserId} 로 사용 가능
    @ModelAttribute("loginUserId")
    public Long loginUserId(HttpSession session) {
        return (Long) session.getAttribute("LOGIN_USER_ID");
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
}

