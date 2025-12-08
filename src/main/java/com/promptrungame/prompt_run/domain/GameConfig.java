package com.promptrungame.prompt_run.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameConfig {

    @Id
    private String ConfigKey; // "MAX_TURN"
    private String ConfigValue; // 11
}
