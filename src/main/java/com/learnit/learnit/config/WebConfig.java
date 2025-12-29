package com.learnit.learnit.config;

import com.learnit.learnit.user.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/home", "/login", "/signup", "/user/additional-info", "/user/find-password",
                        "/css/**", "/js/**", "/images/**", "/files/**", 
                        "/CourseList", "/CourseDetail", "/course/**", "/search", "/error/**", "/oauth2/**", "/api/**");
    }
}

