package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.dto.ChatRequest;
import com.promptrungame.prompt_run.dto.ChatResponse;
import com.promptrungame.prompt_run.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game/chat")
public class ChatController {
    private final GameService gameService;

    public ChatController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping()
    public ResponseEntity<ChatResponse> handleChat(@RequestBody ChatRequest request, HttpSession session) {

        ChatResponse response = gameService.getResponseFromGemini(request.getMessage(), session);

        return ResponseEntity.ok(response);
    }
}
