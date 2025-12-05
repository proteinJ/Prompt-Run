package com.promptrungame.prompt_run.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GameRecordRequest {
    private int hp;
    private String promptUsed;
    private int attemptCount;
    private boolean isSuccess;
    private LocalDateTime playedAt;
    private String end_result;

    private String fullConversationHistory;
}
