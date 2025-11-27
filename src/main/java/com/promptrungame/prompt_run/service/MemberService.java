package com.promptrungame.prompt_run.service;

import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입
    public Member registerMember(Member member) {
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);

        return memberRepository.save(member);
    }

    // 로그인
    public Member loginMember(Member member) {
        Optional<Member> memberOpt = memberRepository.findByUsername(member.getUsername());

        Member foundMember = memberOpt.orElseThrow(() -> new IllegalArgumentException("Not Found User"));

        if (passwordEncoder.matches(member.getPassword(), foundMember.getPassword())) {
            // Login Success
            log.info("로그인 성공");
            return foundMember;
        } else {
            // Login Fail
            log.warn("로그인 실패:: 비밀번호 불일치");
            throw new IllegalArgumentException("비밀번호 불일치");
        }
    }

}
