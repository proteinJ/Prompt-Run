package com.promptrungame.prompt_run.config;
// config 패키지 또는 controller 패키지에 위치

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // MemberService에서 throw 하는 IllegalArgumentException을 잡습니다.
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // HTTP 400 Bad Request 반환
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {

        // 클라이언트에게 오류 메시지와 상태를 JSON 형태로 전달합니다.
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("status", "400");
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage()); // MemberService에서 던진 "비밀번호 불일치" 메시지 포함

        // 400 Bad Request와 JSON 본문을 반환합니다.
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}