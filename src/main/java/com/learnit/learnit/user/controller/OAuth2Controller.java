package com.learnit.learnit.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

/**
 * OAuth2 로그인 콜백 처리
 * 실제 OAuth 인증은 Spring Security의 OAuth2LoginFilter가 처리하고,
 * OAuthService가 사용자 정보를 처리합니다.
 * 
 * 이 컨트롤러는 필요시 추가적인 콜백 처리를 위해 준비된 클래스입니다.
 */
@Controller
@RequiredArgsConstructor
public class OAuth2Controller {

    // OAuth 인증은 Spring Security가 자동으로 처리하므로
    // 특별한 처리가 필요하지 않으면 비워둘 수 있습니다.
    // 필요시 여기에 추가 로직을 구현할 수 있습니다.
}

