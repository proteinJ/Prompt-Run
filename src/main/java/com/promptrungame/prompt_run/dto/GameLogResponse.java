package com.promptrungame.prompt_run.dto;

import com.promptrungame.prompt_run.domain.GameRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GameLogResponse {

    // 공통으로 보여줄 Data
    private Long recordId;
    private String promptUsed;
    private int attemptCount;
    private boolean isSuccess;
    private LocalDateTime playedAt;
    private String end_result;

    // 상세 보기에서만 보여줄 Data
    private int hp;
    private String fullConversationHistory;

    // Entity -> DTO
    public static GameLogResponse from(GameRecord record) {
        return GameLogResponse.builder()
                .recordId(record.getId())
                .promptUsed(record.getPromptUsed())
                .attemptCount(record.getAttemptCount())
                .isSuccess(record.isSuccess())
                .playedAt(record.getPlayedAt())
                .end_result(record.getEndResult())
                .hp(record.getHp())
                .fullConversationHistory(record.getConversationHistory())
                .build();
    }
}
