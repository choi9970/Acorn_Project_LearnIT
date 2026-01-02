package com.learnit.learnit.config;

import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.repository.UserRepository;
import com.learnit.learnit.user.service.SessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        try {
            // Step 1: Authentication에서 정보 추출
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            
            // Step 2: providerId와 email 추출
            String email = extractEmail(oAuth2User, registrationId);
            String providerId = extractProviderId(oAuth2User, registrationId);
            
            if (providerId == null || providerId.isEmpty()) {
                response.sendRedirect("/login?error=true");
                return;
            }

            // Step 3: DB에서 사용자 조회
            User user = userRepository.findByProviderAndProviderId(registrationId, providerId)
                    .orElseGet(() -> {
                        if (email != null && !email.isEmpty()) {
                            return userRepository.findByEmail(email).orElse(null);
                        }
                        return null;
                    });

            if (user == null) {
                response.sendRedirect("/login?error=true");
                return;
            }
            
            // DELETE 상태 사용자는 재가입 처리
            if (User.STATUS_DELETE.equals(user.getStatus())) {
                // 상태를 SIGNUP_PENDING으로 변경하여 재가입 처리
                user.setStatus(User.STATUS_SIGNUP_PENDING);
                user.setUpdatedAt(java.time.LocalDateTime.now());
                
                // OAuth 정보 업데이트 (provider/providerId는 이미 있지만, 최신 정보로 업데이트)
                String name = oAuth2User.getAttribute("name");
                if (name != null) {
                    user.setName((String) name);
                }
                
                String picture = oAuth2User.getAttribute("picture");
                if (picture != null) {
                    user.setProfileImg((String) picture);
                }
                
                userRepository.save(user);
            }
            
            // Step 4: 세션 저장
            HttpSession session = request.getSession(true);
            sessionService.setLoginSession(session, user);
            
            // Step 5: 리다이렉트
            if (User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
                getRedirectStrategy().sendRedirect(request, response, "/user/additional-info");
            } else {
                // 관리자 계정인 경우 관리자 페이지로 리다이렉트
                if ("ADMIN".equals(user.getRole())) {
                    getRedirectStrategy().sendRedirect(request, response, "/admin/home");
                } else {
                    getRedirectStrategy().sendRedirect(request, response, "/home");
                }
            }

        } catch (Exception e) {
            response.sendRedirect("/login?error=true");
        }
    }
    
    private String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        if ("google".equals(registrationId)) {
            Object sub = oAuth2User.getAttribute("sub");
            return sub != null ? sub.toString() : null;
        } else if ("kakao".equals(registrationId)) {
            Object id = oAuth2User.getAttribute("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }
    
    private String extractEmail(OAuth2User oAuth2User, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) oAuth2User.getAttribute("email");
        } else if ("kakao".equals(registrationId)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                return (String) kakaoAccount.get("email");
            }
        }
        return null;
    }
}
