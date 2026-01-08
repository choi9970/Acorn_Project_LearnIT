package com.learnit.learnit.config;

import com.learnit.learnit.user.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${github.api.token:}")
    private String githubToken;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/home", "/login", "/signup", "/user/additional-info", "/user/find-password",
                        "/css/**", "/js/**", "/images/**", "/files/**", 
                        "/CourseList", "/CourseDetail", "/course/**", "/search", "/error/**", "/oauth2/**", "/api/**", "/uploads/**");
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        String projectPath = System.getProperty("user.dir").replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + projectPath + "/uploads/");
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 연결 타임아웃 5초
        factory.setReadTimeout(10000); // 읽기 타임아웃 10초
        
        RestTemplate restTemplate = builder
                .requestFactory(() -> factory)
                .build();
        
        // GitHub API 토큰이 있으면 헤더에 추가
        if (StringUtils.hasText(githubToken)) {
            log.info("GitHub API 토큰이 설정되었습니다. (토큰 길이: {}자, 타입: {})", 
                githubToken.length(), 
                githubToken.startsWith("github_pat_") ? "Fine-grained" : "Classic");
            
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
            interceptors.add(new ClientHttpRequestInterceptor() {
                @Override
                public ClientHttpResponse intercept(
                        HttpRequest request, 
                        byte[] body, 
                        ClientHttpRequestExecution execution) throws IOException {
                    if (request.getURI().toString().contains("api.github.com")) {
                        HttpHeaders headers = request.getHeaders();
                        // Fine-grained token (github_pat_) 또는 Classic token (ghp_) 모두 지원
                        if (githubToken.startsWith("github_pat_")) {
                            headers.set("Authorization", "Bearer " + githubToken);
                            log.debug("GitHub API 요청에 Bearer 토큰 추가: {}", request.getURI());
                        } else {
                            headers.set("Authorization", "token " + githubToken);
                            log.debug("GitHub API 요청에 token 토큰 추가: {}", request.getURI());
                        }
                    }
                    return execution.execute(request, body);
                }
            });
            restTemplate.setInterceptors(interceptors);
        } else {
            log.warn("GitHub API 토큰이 설정되지 않았습니다. 시간당 60회 제한이 적용됩니다.");
        }
        
        return restTemplate;
    }
}

