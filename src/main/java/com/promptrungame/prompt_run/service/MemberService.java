package com.promptrungame.prompt_run.service;

import com.promptrungame.prompt_run.config.JwtTokenProvider;
import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.domain.RefreshToken;
import com.promptrungame.prompt_run.dto.MemberLoginResponse;
import com.promptrungame.prompt_run.dto.MemberProfileResponse;
import com.promptrungame.prompt_run.repository.MemberRepository;
import com.promptrungame.prompt_run.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    // 회원가입
    public Member signupMember(Member member) {
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
            String accessTokenValue = jwtTokenProvider.generateAccessToken(foundMember);
            String refreshTokenValue = jwtTokenProvider.generateRefreshToken(foundMember);

            Date expiresAt = JwtTokenProvider.generateAccessTokenExpiresIn(7);

            if (accessTokenValue == null) {
                log.info("accessToken 생성 실패");
                throw new IllegalArgumentException("accessToken 생성 실패");
            }
            log.info("accessToken 생성 완료");

            // refreshToken을 DB에 저장
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(foundMember.getId())
                    .tokenValue(refreshTokenValue)
                    .memberId(foundMember.getId())
                    .expiredAt(expiresAt)
                    .build();

            refreshTokenRepository.save(refreshToken);
            log.info("refreshToken 저장 완료");

            // 반환할 DTO에 정보 저장
            MemberLoginResponse response = MemberLoginResponse.builder()
                    .memberId(foundMember.getId())
                    .refreshToken(refreshTokenValue)
                    .accessToken(accessTokenValue)
                    .grantType("Bearer")
                    .build();

            log.info("MemberService Login 로직 완료");
            return response;
        } else {
            // Login Fail
            log.warn("로그인 실패:: 비밀번호 불일치");
            throw new IllegalArgumentException("비밀번호 불일치");
        }
    }

    // ##############################
    // ######## (마이페이지) ##########
    // ##############################
    // 사용자 정보 조회
    public MemberProfileResponse  getMemberProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 사용자를 찾을 수 없습니다."));

        return MemberProfileResponse.from(member);
    }

    // 사용자 프로필 사진 변경
    @Transactional
    public String uploadProfileImage(Long memberId, MultipartFile file) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 사용자를 찾을 수 없습니다."));

        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        // 고유한 파일 이름 생성 (UUID 활용)
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFilename = UUID.randomUUID() + extension;

        // 파일 저장
        File targetFile = new File(uploadDir, savedFilename);

        // 업로드 디렉토리가 없으면 생성
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        file.transferTo(targetFile); // 파일 저장 실행

        // 4. 저장된 경로를 DB에 업데이트
        // 실제 웹 접근 URL은 서버 설정에 따라 달라지므로, 여기서는 파일 경로만 저장합니다.
        String imageUrl = "/images/" + savedFilename; // 클라이언트가 접근할 가상 경로 (설정 필요)

        member.setProfileImageUrl(imageUrl); // Member 엔티티에 URL 업데이트

        memberRepository.save(member);

        return imageUrl; // 클라이언트에게 성공적으로 저장된 URL 반환
    }

    // 사용자 요금제(Membership) 변경
    @Transactional
    public void updateMembership(Long memberId, String newMembership) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 1. 멤버십 등급의 유효성 검사 로직 추가 예정

        if (newMembership == null || newMembership.trim().isEmpty()) {
            throw new IllegalArgumentException("새 멤버십 등급을 지정해야 합니다.");
        }
        // * 결제 시스템과의 연동 및 등급 존재 여부 확인 로직 추가 예정*

        // 2. 멤버십 필드 업데이트
        member.setMembership(newMembership.toUpperCase());

        log.info("✅ Member ID {}의 멤버십이 {}로 변경되었습니다.", memberId, newMembership);

        memberRepository.save(member);
    }
}
