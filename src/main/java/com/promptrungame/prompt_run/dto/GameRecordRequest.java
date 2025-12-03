package com.promptrungame.prompt_run.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameRecordRequest {
    private int score;
    private String promptUsed;
    private int attemptCount;
    private boolean isSuccess;
    private LocalDateTime playedAt;
}
