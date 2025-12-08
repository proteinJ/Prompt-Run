package com.promptrungame.prompt_run.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_question")
public class QuizEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question; // 질문

    // DB에는 리스트 저장이 까다로우므로, 콤마(,)나 파이프(|)로 구분된 문자열로 저장합니다.
    // 예: "1. 공격한다 | 2. 방어한다 | 3. 도망친다"
    @Column(nullable = false)
    private String choices;

    @Column(nullable = false)
    private String correctAnswer; // 정답

    private Integer timeLimitSeconds; // 제한 시간 (기본값 설정 가능)

}
