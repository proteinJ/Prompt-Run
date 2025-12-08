package com.promptrungame.prompt_run.repository;

import com.promptrungame.prompt_run.domain.GameConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameConfigRepository extends JpaRepository<GameConfig, String> {
}
