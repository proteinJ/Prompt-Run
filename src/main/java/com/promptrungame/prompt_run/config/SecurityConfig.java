package com.promptrungame.prompt_run.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS/CSRF 보안 관련 설정 (REST API 환경에서는 보통 비활성화)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())

                // HTTP 요청 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 회원가입 및 모든 게임 API 경로를 인증 없이 허용 (Whitelist)
                        .requestMatchers("/api/member/**", "/api/game/**").permitAll()
                        // 나머지 모든 요청은 인증을 요구합니다.
                        .anyRequest().authenticated()
                );

        // 기본 인증 방식을 사용하지 않도록 설정
        // http.httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}
