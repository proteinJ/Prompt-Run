package com.promptrungame.prompt_run.service;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;

import java.util.List;

public interface GeminiClientService {
    String generateContent(String modelName, List<Content> contents, GenerateContentConfig config);
}