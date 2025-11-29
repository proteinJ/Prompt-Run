package com.promptrungame.prompt_run.config;

import com.promptrungame.prompt_run.domain.Member;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;

    private static Date generateAccessTokenExpiresIn(Integer day) {
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

}
