package com.learnit.learnit.user.util;

import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth Provider ID 추출 유틸리티
 */
public class OAuthProviderIdExtractor {

    /**
     * OAuth2User에서 provider와 providerId 추출
     * @param oAuth2User OAuth2User 객체
     * @return ProviderInfo (registrationId, providerId) 또는 null
     */
    public static ProviderInfo extract(OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return null;
        }
        
        // Google: sub 속성이 있으면 Google
        Object subAttr = oAuth2User.getAttribute("sub");
        if (subAttr != null) {
            return new ProviderInfo("google", subAttr.toString());
        }
        
        // Kakao: id 속성이 있으면 Kakao
        Object idObj = oAuth2User.getAttribute("id");
        if (idObj != null) {
            return new ProviderInfo("kakao", idObj.toString());
        }
        
        return null;
    }

    /**
     * Provider 정보를 담는 내부 클래스
     */
    public static class ProviderInfo {
        private final String registrationId;
        private final String providerId;

        public ProviderInfo(String registrationId, String providerId) {
            this.registrationId = registrationId;
            this.providerId = providerId;
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public String getProviderId() {
            return providerId;
        }
    }
}

