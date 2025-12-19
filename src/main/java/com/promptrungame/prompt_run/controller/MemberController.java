package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.dto.MemberLoginResponse;
import com.promptrungame.prompt_run.dto.MemberProfileResponse;
import com.promptrungame.prompt_run.dto.MemberSignupRequest;
import com.promptrungame.prompt_run.dto.MembershipUpdateRequest;
import com.promptrungame.prompt_run.repository.MemberRepository;
import com.promptrungame.prompt_run.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        if (memberRepository.existsByUsername(requestDto.getUsername())) {
            log.warn("❌ 이미 존재하는 아이디");
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        Member member = new Member();
        member.setUsername(requestDto.getUsername());
        member.setNickname(requestDto.getNickname());
        member.setPassword(requestDto.getPassword());
        member.setCreatedAt(LocalDateTime.now());
        member.setMembership("BASIC");
        member.setRole("USER");
        member.setDeleted(false);

        memberService.signupMember(member);

        log.info("✅ 회원가입 성공 username: " + member.getUsername());
        return ResponseEntity.ok("회원가입 성공");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<MemberLoginResponse> loginMember(@RequestBody Member member, HttpServletResponse response, HttpSession session) {
        MemberLoginResponse tokenResponse = memberService.loginMember(member);
        Long memberId = tokenResponse.getMemberId();

        // 참고: STATELESS 환경이라 세션은 큰 의미가 없지만, 기존 로직 유지를 위해 남겨둡니다.
        if (memberId != null) {
            session.setAttribute("memberId", memberId);
            log.info("✅ 로그인 성공 username: " + memberId);
        }

        Cookie refreshTokenCookie = new Cookie("refreshToken", tokenResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7);
        refreshTokenCookie.setPath("/");

        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(tokenResponse);
    }

    //  프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getMyProfile() {
        System.out.println("🚩 컨트롤러 도착! 여기까지 오면 400 아님!");
        // 1. 보안 컨텍스트에서 인증 정보 꺼내기 (가장 확실한 방법)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 로그인 안 된 상태 체크
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("🚨 비로그인 상태에서 프로필 접근");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String memberIdString = authentication.getName();
        Long memberId = Long.parseLong(memberIdString);

        // 아이디로 회원 찾아서 ID(PK) 확보
        MemberProfileResponse response = memberService.getMemberProfile(memberId);

        return ResponseEntity.ok(response);
    }

    // 이미지 업로드
    @PostMapping("/profile/image")
    public ResponseEntity<String> uploadImage(@RequestParam("profileImage") MultipartFile file,
                                              @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("🚨 비로그인 상태에서 프로필 이미지 업로드 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            String memberIdString = userDetails.getUsername();
            Long memberId = Long.parseLong(memberIdString);

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

            String imageUrl = memberService.uploadProfileImage(member.getId(), file);
            return ResponseEntity.ok(imageUrl);

        } catch (IOException e) {
            log.error("파일 업로드 중 I/O 오류 발생", e);
            return ResponseEntity.status(500).body("파일 저장에 실패했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 멤버십 변경
    @PostMapping("/profile/membership")
    public ResponseEntity<String> updateMembership(
            @RequestBody MembershipUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("🚨 비로그인 상태(토큰 없음)에서 Membership 요금제 변경 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String username = userDetails.getUsername();

            memberService.updateMembership(Long.parseLong(username), request.getNewMembership());

            return ResponseEntity.ok("멤버십 등급이 성공적으로 변경되었습니다: " + request.getNewMembership());

        } catch (IllegalArgumentException e) {
            log.warn("멤버십 변경 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("멤버십 변경 중 서버 오류 발생", e);
            return ResponseEntity.status(500).body("멤버십 변경 중 예상치 못한 오류가 발생했습니다.");
        }
    }
}