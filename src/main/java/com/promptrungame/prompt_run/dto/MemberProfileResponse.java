package com.promptrungame.prompt_run.dto;

import com.promptrungame.prompt_run.domain.Member;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberProfileResponse {

    private String username;
    private String nickname;
    private String membership; // BASIC 등급
    private String role;       // USER 권한
    private LocalDateTime createdAt;

    private String profileImageUrl;

    // 엔티티를 DTO로 변환하는 팩토리 메서드
    public static MemberProfileResponse from(Member member) {
        return MemberProfileResponse.builder()
                .username(member.getUsername())
                .nickname(member.getNickname())
                .membership(member.getMembership())

                .role(member.getRole())
                .profileImageUrl(member.getProfileImageUrl())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
