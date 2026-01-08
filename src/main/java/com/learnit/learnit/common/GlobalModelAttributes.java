package com.learnit.learnit.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        // request는 여기서는 항상 들어옴
        return request.getRequestURI(); // 예: /admin/home, /notice, /home ...
    }
}
