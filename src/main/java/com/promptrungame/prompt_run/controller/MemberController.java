package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
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
    public ResponseEntity<Member> loginMember(@RequestBody Member member) {
        Member loginMember = memberService.loginMember(member);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(loginMember);
    }

}
