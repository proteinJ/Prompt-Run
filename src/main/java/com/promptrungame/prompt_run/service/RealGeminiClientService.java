package com.promptrungame.prompt_run.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Profile("!mock") // 🚨 "mock" 모드가 아닐 때만 작동
@Slf4j
public class RealGeminiClientService implements GeminiClientService {

    private final Client geminiClient;

    @Override
    public String generateContent(String modelName, List<Content> contents, GenerateContentConfig config) {
        try {
            // 실제 구글 API 호출
            GenerateContentResponse response = geminiClient.models.generateContent(
                    modelName,
                    contents,
                    config
            );
            return response.text();
        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new RuntimeException("API 호출 중 오류 발생");
        }
    }
}