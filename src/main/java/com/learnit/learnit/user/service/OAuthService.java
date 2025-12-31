package com.learnit.learnit.user.service;

import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.repository.UserRepository;
import com.learnit.learnit.user.dto.OAuthUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        final String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2User oAuth2User = null;
        OAuthUserDTO oauthUser = null;
        User user = null;

        try {
            // Step 1: super.loadUser() 호출
            // 구글의 경우 user-name-attribute가 'sub'인데, API 응답에 'sub'가 없을 수 있으므로
            // 직접 처리하거나 다른 속성을 사용하도록 설정
            if ("google".equals(registrationId)) {
                try {
                    // 먼저 기본 방식으로 시도
                    oAuth2User = super.loadUser(userRequest);
                } catch (IllegalArgumentException e) {
                    // 'sub'가 null인 경우 직접 처리
                    if (e.getMessage() != null && e.getMessage().contains("'sub' cannot be null")) {
                        // 직접 API 호출하여 사용자 정보 가져오기
                        oAuth2User = loadGoogleUserDirectly(userRequest);
                    } else {
                        throw e;
                    }
                } catch (OAuth2AuthenticationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new OAuth2AuthenticationException(
                            new org.springframework.security.oauth2.core.OAuth2Error("oauth_error",
                                    "OAuth 사용자 로드 중 오류: " + e.getMessage(), null), e);
                }
            } else {
                // 구글이 아닌 경우 기본 방식 사용
                try {
                    oAuth2User = super.loadUser(userRequest);
                } catch (OAuth2AuthenticationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new OAuth2AuthenticationException(
                            new org.springframework.security.oauth2.core.OAuth2Error("oauth_error",
                                    "OAuth 사용자 로드 중 오류: " + e.getMessage(), null), e);
                }
            }

            // Step 1 성공 후 검증
            if (oAuth2User == null) {
                String errorMsg = "super.loadUser()가 null을 반환했습니다.";
                throw new OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("oauth_error", errorMsg, null));
            }

            // Step 2: 사용자 정보 추출
            try {
                oauthUser = extractOAuthUserInfo(oAuth2User, registrationId);
            } catch (Exception e) {
                throw new OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("oauth_error",
                                "사용자 정보 추출 중 오류: " + e.getMessage(), null), e);
            }

            if (oauthUser == null) {
                String errorMsg = "extractOAuthUserInfo()가 null을 반환했습니다.";
                throw new OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("oauth_error", errorMsg, null));
            }

            // Step 3: 사용자 찾기 또는 생성
            try {
                user = findOrCreateUser(registrationId, oauthUser);
            } catch (Exception e) {
                throw new OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("oauth_error",
                                "사용자 찾기/생성 중 오류: " + e.getMessage(), null), e);
            }

            if (user == null) {
                String errorMsg = "findOrCreateUser()가 null을 반환했습니다.";
                throw new OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("oauth_error", errorMsg, null));
            }

            return oAuth2User;
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error("oauth_error",
                            "OAuth 로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), null), e);
        }
    }

    /**
     * OAuth 제공자별 사용자 정보 추출
     */
    private OAuthUserDTO extractOAuthUserInfo(OAuth2User oAuth2User, String registrationId) {
        OAuthUserDTO dto = new OAuthUserDTO();
        dto.setProvider(registrationId);

        if ("google".equals(registrationId)) {
            // 구글은 'sub'가 고유 ID입니다.
            Object sub = oAuth2User.getAttribute("sub");
            dto.setProviderId(sub != null ? sub.toString() : null);

            Object emailObj = oAuth2User.getAttribute("email");
            dto.setEmail((String) emailObj);

            Object nameObj = oAuth2User.getAttribute("name");
            dto.setName((String) nameObj);

            Object pictureObj = oAuth2User.getAttribute("picture");
            dto.setProfileImg((String) pictureObj);
        } else if ("kakao".equals(registrationId)) {
            Object idObj = oAuth2User.getAttribute("id");
            dto.setProviderId(idObj != null ? idObj.toString() : null);

            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttribute("kakao_account");

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

        if (dto.getProviderId() == null || dto.getProviderId().isEmpty()) {
            // providerId가 없으면 예외 발생하지 않고 null 반환
        }

        return dto;
    }

    /**
     * 기존 사용자 찾기 또는 새 사용자 생성
     */
    @Transactional
    private User findOrCreateUser(String registrationId, OAuthUserDTO oauthUser) {
        // Step 1: providerId 검증
        if (oauthUser.getProviderId() == null || oauthUser.getProviderId().isEmpty()) {
            throw new IllegalArgumentException("providerId를 추출할 수 없습니다.");
        }

        // Step 2: 기존 사용자 조회
        User user = userRepository.findByProviderAndProviderId(registrationId, oauthUser.getProviderId())
                .orElse(null);

        if (user != null) {
            return user;
        }

        // Step 3: 새 사용자 생성
        User newUser = new User();

        // 이메일 설정 (NOT NULL UNIQUE 제약 조건 고려)
        String email;
        if (oauthUser.getEmail() != null && !oauthUser.getEmail().isEmpty()) {
            // OAuth에서 이메일을 제공한 경우
            email = oauthUser.getEmail();

            // 이메일 중복 체크 (UNIQUE 제약 조건)
            if (userRepository.existsByEmail(email)) {
                // 중복된 이메일을 가진 사용자가 같은 provider/providerId를 가진 경우는 기존 사용자 반환
                User existingUserByEmail = userRepository.findByEmail(email).orElse(null);
                if (existingUserByEmail != null &&
                        registrationId.equals(existingUserByEmail.getProvider()) &&
                        oauthUser.getProviderId().equals(existingUserByEmail.getProviderId())) {
                    return existingUserByEmail;
                }

                // 다른 provider/providerId를 가진 경우, 고유한 이메일 생성
                email = registrationId + "_" + oauthUser.getProviderId() + "@" + registrationId + ".com";
            }
        } else {
            // OAuth에서 이메일을 제공하지 않은 경우
            // provider_providerId@registrationId.com 형태로 고유 이메일 생성
            email = registrationId + "_" + oauthUser.getProviderId() + "@" + registrationId + ".com";

            // 생성한 이메일도 중복 체크 (거의 없겠지만 안전을 위해)
            int suffix = 1;
            while (userRepository.existsByEmail(email)) {
                email = registrationId + "_" + oauthUser.getProviderId() + "_" + suffix + "@" + registrationId + ".com";
                suffix++;
                if (suffix > 1000) { // 무한 루프 방지
                    throw new RuntimeException("고유한 이메일을 생성할 수 없습니다.");
                }
            }
        }

        newUser.setEmail(email);

        // 이름 설정
        String name = oauthUser.getName() != null && !oauthUser.getName().isEmpty()
                ? oauthUser.getName() : "사용자";
        newUser.setName(name);

        // 기타 필드 설정
        newUser.setNickname(null);
        newUser.setProvider(registrationId);
        newUser.setProviderId(oauthUser.getProviderId());
        newUser.setProfileImg(oauthUser.getProfileImg());
        newUser.setRole("USER");
        newUser.setStatus(User.STATUS_SIGNUP_PENDING);
        newUser.setPhone(null);
        newUser.setRegion(null);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // Step 4: DB 저장
        try {
            newUser = userRepository.save(newUser);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("사용자 저장 중 DB 제약 조건 위반: " + e.getMessage(), e);
        } catch (Exception e) {
            throw e;
        }

        return newUser;
    }

    /**
     * 구글 API 응답을 직접 처리하여 OAuth2User 생성
     * 'sub' 속성이 없을 때 사용
     */
    private OAuth2User loadGoogleUserDirectly(OAuth2UserRequest userRequest) {
        try {
            // Access Token 가져오기
            String accessToken = userRequest.getAccessToken().getTokenValue();

            // 구글 API 호출
            RestTemplate restTemplate = new RestTemplate();
            String userInfoUri = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(accessToken);
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    userInfoUri,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> attributes = response.getBody();

            if (attributes == null) {
                throw new OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_response",
                                "구글 API 응답이 null입니다.", null));
            }

            // 'sub'가 없으면 'id'를 사용하거나 생성
            if (!attributes.containsKey("sub") || attributes.get("sub") == null) {
                // 'id'가 있으면 'sub'로 복사
                if (attributes.containsKey("id") && attributes.get("id") != null) {
                    attributes.put("sub", attributes.get("id"));
                } else {
                    // 'id'도 없으면 email 기반으로 생성
                    String email = (String) attributes.get("email");
                    if (email != null) {
                        String generatedSub = "google_" + email.hashCode();
                        attributes.put("sub", generatedSub);
                    } else {
                        throw new OAuth2AuthenticationException(
                                new org.springframework.security.oauth2.core.OAuth2Error("invalid_response",
                                        "구글 API 응답에 'sub' 또는 'id' 또는 'email'이 없습니다.", null));
                    }
                }
            }

            // DefaultOAuth2User 생성
            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            OAuth2User oAuth2User = new DefaultOAuth2User(
                    org.springframework.security.core.authority.AuthorityUtils.NO_AUTHORITIES,
                    attributes,
                    userNameAttributeName
            );

            return oAuth2User;
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error("oauth_error",
                            "구글 사용자 정보 로드 중 오류: " + e.getMessage(), null), e);
        }
    }
}