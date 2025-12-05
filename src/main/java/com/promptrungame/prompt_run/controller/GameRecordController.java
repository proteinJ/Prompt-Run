package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.domain.GameRecord;
import com.promptrungame.prompt_run.dto.GameLogResponse;
import com.promptrungame.prompt_run.service.GameRecordService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class GameRecordController {

    private final GameRecordService gameRecordService;
    private static final String MEMBER_ID_SESSION_KEY = "memberId";

    @GetMapping
    public ResponseEntity<List<GameLogResponse>> getMyGameLog(HttpSession session) {
        Long memberId = (Long) session.getAttribute(MEMBER_ID_SESSION_KEY);

        List<GameRecord> gameRecords = gameRecordService.getMyGameRecords(memberId);

        List<GameLogResponse> responseList = gameRecords.stream()
                // GameRecordService.getMyGameRecords가 Member 엔티티를 FETCH JOIN하지 않으면 N+1 문제가 발생할 수 있습니다.
                // 쿼리 최적화는 추후 고려하고, 일단 변환합니다.
                .map(GameLogResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }


}
