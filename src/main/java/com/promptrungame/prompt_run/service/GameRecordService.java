package com.promptrungame.prompt_run.service;

import com.promptrungame.prompt_run.domain.GameRecord;
import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.dto.GameRecordRequest;
import com.promptrungame.prompt_run.repository.GameRecordRepository;
import com.promptrungame.prompt_run.repository.MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameRecordService {

    private final GameRecordRepository gameRecordRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public GameRecord saveGameRecord(Long memberId, GameRecordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        GameRecord gameRecord = GameRecord.builder()
                .member(member)
                .hp(request.getHp())
                .promptUsed(request.getPromptUsed())
                .attemptCount(request.getAttemptCount())
                .isSuccess(request.isSuccess())
                .playedAt(request.getPlayedAt())
                .endResult(request.getEnd_result())
                .conversationHistory(request.getFullConversationHistory())
                .build();

        return gameRecordRepository.save(gameRecord);
    }

    public List<GameRecord> getMyGameRecords(Long memberId) {
        return gameRecordRepository.findByMemberIdOrderByPlayedAtDesc(memberId);
    }
}
