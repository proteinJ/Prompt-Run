package com.promptrungame.prompt_run.service;

import com.promptrungame.prompt_run.config.JwtTokenProvider;
import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.dto.MemberLoginResponse;
import com.promptrungame.prompt_run.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    public Member registerMember(Member member) {
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);

        return memberRepository.save(member);
    }

    // 로그인
    public MemberLoginResponse loginMember(Member member) {
        Optional<Member> memberOpt = memberRepository.findByUsername(member.getUsername());

        Member foundMember = memberOpt.orElseThrow(() -> new IllegalArgumentException("Not Found User"));

        if (passwordEncoder.matches(member.getPassword(), foundMember.getPassword())) {
            // Login Success
            String accessToken = jwtTokenProvider.generateAccessToken(foundMember);
            if (accessToken == null) {
                log.info("accessToken 생성 실패");
                throw new IllegalArgumentException("accessToken 생성 실패");
            }
            log.info("accessToken 생성 완료");

            // refreshToken을 DB에 저장


            // 반환할 DTO에 정보 저장
            MemberLoginResponse response = MemberLoginResponse.builder()
                    .memberId(foundMember.getId())
                    .accessToken(accessToken)
                    .grantType("Bearer")
                    .build();

            log.info("로그인 성공");
            return response;
        } else {
            // Login Fail
            log.warn("로그인 실패:: 비밀번호 불일치");
            throw new IllegalArgumentException("비밀번호 불일치");
        }
    }

}
