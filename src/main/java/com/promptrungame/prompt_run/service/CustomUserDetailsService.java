package com.promptrungame.prompt_run.service;

import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // MemberRepository를 주입받아 DB에서 사용자를 조회합니다.
    private final MemberRepository memberRepository;

    // Spring Security가 인증을 위해 사용자 이름(username)을 요청할 때 호출됩니다.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. DB에서 사용자 정보(Member Entity)를 가져옵니다.
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 2. [권한 처리] DB에서 가져온 role을 Spring Security 표준으로 변환합니다.
        String dbRole = member.getRole();
        String finalAuthority;

        // DB에 'ROLE_' 접두사가 붙어있지 않다면 Java에서 붙임.
        if (dbRole == null || dbRole.isEmpty() || dbRole.startsWith("ROLE_")) {
            finalAuthority = dbRole;
        } else {
            finalAuthority = "ROLE_" + dbRole; // "ADMIN" -> "ROLE_ADMIN"
        }

        // 3. GrantedAuthority 리스트 생성
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(finalAuthority)
        );

        // 4. Spring Security의 UserDetails 객체(User)로 변환하여 반환합니다.
        // Spring Security의 User 객체는 비밀번호, 사용자 이름, 권한 리스트를 필수로 요구합니다.
        return new User(
                member.getUsername(),
                member.getPassword(), // 암호화된 비밀번호가 전달되어야 함
                authorities
        );
    }
}