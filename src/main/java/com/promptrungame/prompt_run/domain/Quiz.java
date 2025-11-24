package com.promptrungame.prompt_run.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
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

}
