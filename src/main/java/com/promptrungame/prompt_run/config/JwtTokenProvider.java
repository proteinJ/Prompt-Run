package com.promptrungame.prompt_run.config;

import com.promptrungame.prompt_run.domain.Member;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

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

        // DB에서 가져온 role 값을 준비합니다. (예: "ADMIN")
        String memberRole = member.getRole();

        // 1. ROLE_ 접두사 처리 (DB에 'ADMIN'만 있을 경우 대비)
        if (!memberRole.startsWith("ROLE_")) {
            memberRole = "ROLE_" + memberRole;
        }

        String authorities = memberRole;

        return Jwts.builder()
                .setSubject(String.valueOf(member.getId())) // 필수
                .claim("username", member.getUsername()) // 사용자 정의: 이름
                .claim("auth", authorities)
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
        String authoritiesString = claims.get("auth", String.class);

        // 3. 권한 문자열을 SimpleGrantedAuthority 객체 리스트로 변환
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(authoritiesString.split(",")) // 쉼표로 분리
                        .map(String::trim) // 공백 제거
                        .filter(auth -> !auth.isEmpty()) // 빈 문자열 필터링
                        .map(SimpleGrantedAuthority::new) // GrantedAuthority로 포장
                        .collect(Collectors.toList());

        // 4. UserDetails 객체 생성 (principal)
        // Spring Security 컨텍스트에 사용자 정보(username)와 추출된 권한을 담습니다.
        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                claims.getSubject(),
                "",          // 토큰 기반이므로 비밀번호는 비워둡니다.
                authorities          // 토큰에서 추출한 권한 리스트 사용!
        );

        // 5. Authentication 객체 반환
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

}
