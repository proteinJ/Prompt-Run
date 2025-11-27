package com.promptrungame.prompt_run.config;

import com.promptrungame.prompt_run.domain.Member;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private static final long MILLISECONDS_PER_DAY = 1000L * 60 * 60 * 24; // 1일

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        // application.properties에서 시크릿 키를 로드
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Member member) {
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + MILLISECONDS_PER_DAY);

        return Jwts.builder()
                .setSubject(String.valueOf(member.getId()))
                .claim("username", member.getUsername())
                .setExpiration(accessTokenExpiresIn)
                .compact();
    }

}
