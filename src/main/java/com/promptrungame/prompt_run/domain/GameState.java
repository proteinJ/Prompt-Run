package com.promptrungame.prompt_run.domain;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    private String theme;
    private int hp;
    private int currentTurn;
    private OffsetDateTime startedAt;
    private OffsetDateTime lastUpdatedAt;
    private boolean quizPending;

    @Builder.Default
    private List<String> history = new ArrayList<>();
}
