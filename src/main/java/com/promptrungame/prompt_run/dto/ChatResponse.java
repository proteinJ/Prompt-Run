package com.promptrungame.prompt_run.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    // User에게 보여줄 가공된 Text
    private String response;

    // Model의 원본 응답
    private String rawResponse;

    // 퀴즈 요청 여부
    private boolean isQuizRequest;

    // 퀴즈 Data
    private Quiz quizEntityData;

    // 게임 종료 여부
    private  boolean isGameEnded;

    // 결과 태그
    private String resultTag;

    // 에러 메시지
     private String error;
}
