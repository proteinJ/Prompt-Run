package com.promptrungame.prompt_run.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ClientConfig {
    @Bean
    public Client geminiClient() {
        return new Client();
    }
}
