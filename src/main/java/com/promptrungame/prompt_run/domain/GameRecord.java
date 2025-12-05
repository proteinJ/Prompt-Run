package com.promptrungame.prompt_run.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 게임 관련 데이터
    private int hp;
    private String promptUsed;
    private int attemptCount;
    private boolean isSuccess;
    private String endResult;
    private LocalDateTime playedAt;

    @Column(columnDefinition = "TEXT")
    private String conversationHistory;

}
