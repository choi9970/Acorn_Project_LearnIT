package com.learnit.learnit.config;

import com.learnit.learnit.cart.CartService;
import com.learnit.learnit.cart.GuestCartService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final SessionService sessionService;

    // ✅ 게스트 장바구니 병합용(이미 적용했다면 그대로)
    private final CartService cartService;
    private final GuestCartService guestCartService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        HttpSession session = request.getSession(true);

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = ((OAuth2AuthenticationToken) authentication)
                    .getAuthorizedClientRegistrationId();

            // ✅ login?redirect=... 로 들어온 값(세션 저장해둔 것) 최우선 처리
            String redirect = (String) session.getAttribute("REDIRECT_AFTER_LOGIN");
            session.removeAttribute("REDIRECT_AFTER_LOGIN");

            String email = extractEmail(oAuth2User, registrationId);
            String providerId = extractProviderId(oAuth2User, registrationId);

            if (providerId == null || providerId.isEmpty()) {
                response.sendRedirect("/login?error=true");
                return;
            }

            // 1) provider+providerId로 조회
            // 2) 없으면 email로 조회(있으면)
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

            // ✅ 탈퇴 상태면 재가입 플로우로 전환(기존 로직 유지)
            if (User.STATUS_DELETE.equals(user.getStatus())) {
                user.setStatus(User.STATUS_SIGNUP_PENDING);
                user.setUpdatedAt(LocalDateTime.now());

                String name = oAuth2User.getAttribute("name");
                if (name != null) user.setName(name);

                String picture = oAuth2User.getAttribute("picture");
                if (picture != null) user.setProfileImg(picture);

                userRepository.save(user);
            }

            // ✅ 로그인 세션 세팅
            sessionService.setLoginSession(session, user);
            @SuppressWarnings("unchecked")
            List<Long> guestCourseIds =
                    (List<Long>) session.getAttribute("GUEST_CART_COURSE_IDS");

            if (guestCourseIds != null && !guestCourseIds.isEmpty()) {
                cartService.mergeGuestCartToUser(user.getUserId(), guestCourseIds);
                session.removeAttribute("GUEST_CART_COURSE_IDS");
            }



            // ✅ 추가정보 입력 페이지 우선
            if (User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
                getRedirectStrategy().sendRedirect(request, response, "/user/additional-info");
                return;
            }

            // ✅✅ redirect가 있으면 최우선(안전 체크)
            if (isSafeRedirect(redirect)) {
                getRedirectStrategy().sendRedirect(request, response, redirect);
                return;
            }

            // ✅ 기존 기본 이동 로직
            if ("ADMIN".equals(user.getRole())) {
                getRedirectStrategy().sendRedirect(request, response, "/admin/home");
            } else {
                getRedirectStrategy().sendRedirect(request, response, "/home");
            }

        } catch (Exception e) {
            // 예외 시 로그인 페이지로
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

    @SuppressWarnings("unchecked")
    private String extractEmail(OAuth2User oAuth2User, String registrationId) {
        if ("google".equals(registrationId)) {
            return oAuth2User.getAttribute("email");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                Object email = kakaoAccount.get("email");
                return email != null ? email.toString() : null;
            }
        }
        return null;
    }

    // ✅ Open Redirect 방지: 내부 경로만 허용
    private boolean isSafeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank()) return false;
        if (!redirect.startsWith("/")) return false;
        if (redirect.startsWith("//")) return false;
        if (redirect.contains("http://") || redirect.contains("https://")) return false;
        return true;
    }
}





