package com.promptrungame.prompt_run.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 1. resourceLocation 문자열 정리
        String resourceLocation = uploadDir;
        if (!resourceLocation.endsWith("/") && !resourceLocation.endsWith("\\")) {
            resourceLocation += "/";
        }

        // 2. Windows 경로 안정화: "file:///" 접두사 사용
        // file:///C:/... 형태로 만들어 줍니다.
        // Spring Boot는 알아서 Windows 경로를 처리해주지만, 명시적으로 file:을 사용하는 것이 더 안전합니다.
        String mappingPath = "file:///" + resourceLocation;

        // 3. /images/** 요청을 물리적 폴더로 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations(mappingPath);
    }
}