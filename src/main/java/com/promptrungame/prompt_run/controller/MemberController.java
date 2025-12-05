package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.dto.MemberLoginResponse;
import com.promptrungame.prompt_run.dto.MemberSignupRequest;
import com.promptrungame.prompt_run.repository.MemberRepository;
import com.promptrungame.prompt_run.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Slf4j
public class MemberController {
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> registerMember(@Valid @RequestBody MemberSignupRequest requestDto) {

        // 1. 중복 체크
        if (memberRepository.existsByUsername(requestDto.getUsername())) {
            log.warn("❌ 이미 존재하는 아이디");
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        // 2. DTO -> Entity 변환 및 기본값 세팅
        Member member = new Member();
        member.setUsername(requestDto.getUsername());
        member.setNickname(requestDto.getNickname());

        // 임시 비밀번호 설정: 인코딩을 위해 DTO에서 받은 raw password를 일단 엔티티에 설정
        member.setPassword(requestDto.getPassword());

        // 기본값 세팅
        member.setCreatedAt(LocalDateTime.now());
        member.setMembership("BASIC");    // 기본 등급
        member.setRole("USER");      // 기본 권한
        member.setDeleted(false);

        // 3. 서비스 호출 (비밀번호 인코딩 및 DB 저장)
        memberService.signupMember(member);

        log.info("✅ 회원가입 성공 username: " + member.getUsername());
        return ResponseEntity.ok("회원가입 성공");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<MemberLoginResponse> loginMember(@RequestBody Member member, HttpServletResponse response, HttpSession session) {
        MemberLoginResponse tokenResponse = memberService.loginMember(member);

        Long memberId = tokenResponse.getMemberId();

        if (memberId != null) {
            session.setAttribute("memberId", memberId);
            log.info("✅ 로그인 성공 username: " + memberId);
        }

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
