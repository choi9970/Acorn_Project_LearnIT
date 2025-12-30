package com.learnit.learnit.mypage.util;

/**
 * GitHub URL 관련 유틸리티 클래스
 */
public class GitHubUrlUtils {
    
    /**
     * GitHub URL을 표준 형식으로 변환합니다.
     * 사용자명만 입력된 경우 자동으로 https://github.com/ 형식으로 변환합니다.
     * 
     * @param input GitHub URL 또는 사용자명
     * @return 표준화된 GitHub URL, 입력이 null이거나 비어있으면 null
     */
    public static String formatGitHubUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        String url = input.trim();
        
        // 이미 URL 형식인 경우 그대로 반환
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        
        // github.com/ 형식인 경우 https:// 추가
        if (url.startsWith("github.com/")) {
            return "https://" + url;
        }
        
        // 사용자명만 입력된 경우 https://github.com/ 추가
        return "https://github.com/" + url;
    }
    
    /**
     * GitHub URL에서 사용자명을 추출합니다.
     * 
     * @param githubUrl GitHub URL
     * @return 사용자명, 추출할 수 없으면 null
     */
    public static String extractUsername(String githubUrl) {
        if (githubUrl == null || githubUrl.trim().isEmpty()) {
            return null;
        }

        String url = githubUrl.trim();
        
        // https://github.com/username 또는 github.com/username 형식 처리
        if (url.contains("github.com/")) {
            String[] parts = url.split("github.com/");
            if (parts.length > 1) {
                String username = parts[1].split("/")[0].split("\\?")[0];
                return username.trim();
            }
        }
        
        return null;
    }
}

