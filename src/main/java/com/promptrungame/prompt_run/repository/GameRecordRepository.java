package com.promptrungame.prompt_run.repository;

import com.promptrungame.prompt_run.domain.GameRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {
    List<GameRecord> findByMemberIdOrderByPlayedAtDesc(Long memberId);
}
