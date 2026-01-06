package com.learnit.learnit.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 공용 모델 데이터 제공
 * - 로그인 redirect용 currentUrl
 * - 공용 fragment(header 등)에서 request 직접 접근 금지
 */
@ControllerAdvice
public class CommonModelAdvice {

    /**
     * 현재 요청 URL (queryString 포함)
     * ex) /CourseDetail?courseId=3&tab=reviews
     */
    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        if (request == null) return "/";

        String uri = request.getRequestURI();
        String qs = request.getQueryString();

        if (qs == null || qs.isBlank()) {
            return uri;
        }
        return uri + "?" + qs;
    }
}
