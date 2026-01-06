package com.learnit.learnit.config;

import com.learnit.learnit.user.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuthService oAuthService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/home", "/login", "/signup", "/user/additional-info", "/user/find-password",
                                "/css/**", "/js/**", "/images/**", "/files/**", "/uploads/**", "/notice/**",
                                "/CourseList", "/CourseDetail", "/course/**", "/search", "/error/**",
                                "/api/user/check-email", "/api/courses", "/api/search/**",
                                "/api/mypage/github/**", "/mypage/**",
                                "/oauth2/authorization/**", "/login/oauth2/code/**",
                                "/cart/**", "/payment/**", "/payments/**",
                                "/admin/**","/api/chatbot/**","/notice", "/notice/**","/api/reviews/**").permitAll() // Security는 통과시키고, 실제 검증은 AuthInterceptor에게 위임
                        .anyRequest().authenticated()
                )
            .oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .successHandler(oAuth2LoginSuccessHandler) // 커스텀 핸들러 사용 (SIGNUP_PENDING 체크 및 리다이렉트 처리)
                    .failureUrl("/login?error=true")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(oAuthService)
                    )
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/home")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}