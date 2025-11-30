package com.promptrungame.prompt_run.config;

import com.promptrungame.prompt_run.domain.Member;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final Key key;

    public static Date generateAccessTokenExpiresIn(Integer day) {
        final long MILLISECONDS_PER_DAY = 1000L * 60 * 60 * 24;
        long now = (new Date()).getTime();

        return new Date(now + MILLISECONDS_PER_DAY * day);
    }

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        // application.properties에서 시크릿 키를 로드
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Member member) {
        Date accessTokenExpiresIn = generateAccessTokenExpiresIn(1);

        return Jwts.builder()
                .setSubject(String.valueOf(member.getId())) // 필수
                .claim("username", member.getUsername()) // 사용자 정의: 이름
                .setExpiration(accessTokenExpiresIn) // 필수
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Member member) {
        Date refreshTokenExpiresIn = generateAccessTokenExpiresIn(7);

        return Jwts.builder()
                .setSubject(String.valueOf(member.getId())) // 필수
                .claim("username", member.getUsername()) // 사용자 정의: 이름
                .setExpiration(refreshTokenExpiresIn) // 필수
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 1. HTTP 요청 헤더에서 토큰을 추출하는 메서드
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer" 문자열 제거
        }
        return null;
    }

    // 2. 토큰의 유효성을 검사하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // 잘못된 JWT 서명
        } catch (ExpiredJwtException e) {
            // 만료된 JWT 토큰
        } catch (UnsupportedJwtException e) {
            // 지원되지 않는 JWT 토큰
        } catch (IllegalArgumentException e) {
            // JWT 토큰이 잘못됨
        }
        return false;
    }

    // 3. 토큰에서 인증 정보를 조회하는 메서드
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        // 이 예시에서는 권한이 "ROLE_USER" 하나라고 가정합니다.
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                claims.getSubject(), // 토큰의 Subject (여기서는 Member ID)를 Username으로 사용
                "",                  // 토큰 기반 인증이므로 비밀번호는 비워둡니다.
                authorities
        );

        // 인증 객체(Authentication) 반환
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

}
