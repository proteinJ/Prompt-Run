package com.promptrungame.prompt_run.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long memberId;
    private String grantType;
}
