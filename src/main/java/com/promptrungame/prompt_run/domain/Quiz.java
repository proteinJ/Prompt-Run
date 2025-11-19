package com.promptrungame.prompt_run.domain;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class Quiz {
    private String question;
    private List<String> choices;
    private String correctAnswer;
    private OffsetDateTime issuedAt;
    private Integer timeLimitSeconds;

    public boolean validateAnswer(String ans) {
        if (ans == null || correctAnswer == null)
            return false;

        return ans.trim().equalsIgnoreCase(correctAnswer);
    }
}
