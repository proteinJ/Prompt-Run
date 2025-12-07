package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.domain.GameRecord;
import com.promptrungame.prompt_run.dto.GameLogResponse;
import com.promptrungame.prompt_run.service.GameRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
//    private static final String MEMBER_ID_SESSION_KEY = "memberId";

    @GetMapping
    public ResponseEntity<?> getMyGameLog(@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        Long memberId = Long.parseLong(userDetails.getUsername());

        try {
            List<GameRecord> gameRecords = gameRecordService.getMyGameRecords(memberId);

            List<GameLogResponse> responseList = gameRecords.stream()
                    .map(GameLogResponse::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);

        } catch (NumberFormatException e) {
            log.error("토큰 ID 파싱 에러", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 인증 정보입니다.");
        }
    }
}
