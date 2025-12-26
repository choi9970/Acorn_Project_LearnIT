package com.learnit.learnit.user.service;

import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.mapper.UserMapper;
import com.learnit.learnit.user.dto.OAuthUserDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        final String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUserDTO oauthUser = extractOAuthUserInfo(oAuth2User, registrationId);
        
        // 기존 사용자 찾기 또는 새 사용자 생성
        User user = findOrCreateUser(registrationId, oauthUser);
        
        // 세션에 사용자 정보 저장
        setLoginSession(user);
        
        return oAuth2User;
    }

    /**
     * OAuth 제공자별 사용자 정보 추출
     */
    private OAuthUserDTO extractOAuthUserInfo(OAuth2User oAuth2User, String registrationId) {
        OAuthUserDTO dto = new OAuthUserDTO();
        dto.setProvider(registrationId);
        
        if ("google".equals(registrationId)) {
            dto.setProviderId(oAuth2User.getAttribute("sub"));
            dto.setEmail(oAuth2User.getAttribute("email"));
            dto.setName(oAuth2User.getAttribute("name"));
            dto.setProfileImg(oAuth2User.getAttribute("picture"));
        } else if ("kakao".equals(registrationId)) {
            Object idObj = oAuth2User.getAttribute("id");
            dto.setProviderId(idObj != null ? idObj.toString() : null);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                dto.setEmail((String) kakaoAccount.get("email"));
                
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    dto.setName((String) profile.get("nickname"));
                    dto.setProfileImg((String) profile.get("profile_image_url"));
                }
            }
        }
        
        return dto;
    }

    /**
     * 기존 사용자 찾기 또는 새 사용자 생성
     */
    @Transactional
    private User findOrCreateUser(String registrationId, OAuthUserDTO oauthUser) {
        User user = userMapper.selectUserByProviderAndProviderId(registrationId, oauthUser.getProviderId());
        
        if (user == null) {
            // 새 사용자 생성 (추가 정보 미완료 상태)
            User newUser = new User();
            newUser.setEmail(oauthUser.getEmail() != null 
                    ? oauthUser.getEmail() 
                    : oauthUser.getProviderId() + "@" + registrationId + ".com");
            newUser.setName(oauthUser.getName() != null ? oauthUser.getName() : "사용자");
            newUser.setProvider(registrationId);
            newUser.setProviderId(oauthUser.getProviderId());
            newUser.setProfileImg(oauthUser.getProfileImg());
            newUser.setRole("USER");
            newUser.setStatus(User.STATUS_SIGNUP_PENDING); // 추가 정보 입력 전이므로 SIGNUP_PENDING
            // phone, region은 null로 유지 (추가 정보 입력 필요)
            newUser.setPhone(null);
            newUser.setRegion(null);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            userMapper.insertUser(newUser);
            return newUser;
        }
        
        return user;
    }

    /**
     * 세션에 로그인 정보 저장
     */
    private void setLoginSession(User user) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession();
        session.setAttribute("LOGIN_USER_ID", user.getUserId());
        session.setAttribute("LOGIN_USER_NAME", user.getName());
        session.setAttribute("LOGIN_USER_ROLE", user.getRole());
    }
}

