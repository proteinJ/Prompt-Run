package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.dto.MemberLoginResponse;
import com.promptrungame.prompt_run.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<Member> registerMember(@RequestBody Member member) {

        Member savedMember = memberService.registerMember(member);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedMember);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<MemberLoginResponse> loginMember(@RequestBody Member member, HttpServletResponse response) {
        MemberLoginResponse tokenResponse = memberService.loginMember(member);

        // refreshToken을 HTTP-Only Cookie에 저장
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokenResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7); // 유효기간 7일
        refreshTokenCookie.setPath("/");

        response.addCookie(refreshTokenCookie);

        return ResponseEntity
                .ok(tokenResponse);
    }
}
