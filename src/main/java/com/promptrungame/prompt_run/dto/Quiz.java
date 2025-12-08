package com.promptrungame.prompt_run.dto;

import com.promptrungame.prompt_run.domain.QuizEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz implements Serializable {

    private String question;
    private List<String> choices;
    private String correctAnswer;
    private OffsetDateTime issuedAt;
    private Integer timeLimitSeconds;

    //  Entity -> DTO 변환
    public static Quiz from(QuizEntity entity) {
        String rawChoices = entity.getChoices();
        // DB에 "A | B | C" 형태로 저장된 문자열을 잘라서 리스트로 만듦
        List<String> parsedChoices = Arrays.stream(rawChoices.split("\\|"))
                .map(String::trim)
                .toList();

        return Quiz.builder()
                .question(entity.getQuestion())
                .choices(parsedChoices)
                .correctAnswer(entity.getCorrectAnswer())
                .timeLimitSeconds(entity.getTimeLimitSeconds())
                .issuedAt(OffsetDateTime.now()) // 출제 시간은 현재 시간으로 설정
                .build();
    }
}
