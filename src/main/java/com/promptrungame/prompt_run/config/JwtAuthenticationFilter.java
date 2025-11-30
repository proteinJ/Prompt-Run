package com.promptrungame.prompt_run.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component // Bean으로 등록
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청에서 토큰 추출
        String token = jwtTokenProvider.resolveToken(request);

        // 2. 토큰이 존재하고 유효한지 검증
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰이 유효하면 인증 정보 가져오기
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // 4. SecurityContext에 인증 정보 저장 (인증 완료 처리)
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response); // 다음 필터로 진행
    }

    /**
     * 특정 경로에서는 필터링을 건너뛰도록 설정
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // '/api/member'로 시작하는 경로는 회원가입/로그인으로 간주하고 필터링 건너뛰기
        if (path.startsWith("/api/member/register") || path.startsWith("/api/member/login")) {
            return true;
        }

        return false;
    }
}