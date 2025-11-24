package com.promptrungame.prompt_run.service;

import com.promptrungame.prompt_run.domain.Quiz;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class QuizService {

    // QUIZ 하드코딩(문제정의) -> 추후에 Repository에서 가져올 것.
    private static final List<Quiz> HARDCODING_QUIZ = Arrays.asList(
            Quiz.builder()
                    .question("A와 B중 어느것?")
                    .choices(List.of("A. ...", "B. ..."))
                    .correctAnswer("A")
                    .build(),
            Quiz.builder()
                    .question("C와 D중 어느것?")
                    .choices(List.of("C. ...", "D. ..."))
                    .correctAnswer("C")
                    .build(),
            Quiz.builder()
                    .question("E와 F중 어느것?")
                    .choices(List.of("E. ...", "F. ..."))
                    .correctAnswer("F")
                    .build()
    );

    private final Random random = new Random();
    private static final String QUIZ_SESSION_KEY = "PromptRunQuiz";

    // 하드코딩된 목록에서 Random QUIZ 선택하여 세션에 저장하고 반환.
    public Quiz issueNewQuiz(HttpSession session) {
        int index = random.nextInt(HARDCODING_QUIZ.size());
        Quiz quiz = HARDCODING_QUIZ.get(index);

        session.setAttribute(QUIZ_SESSION_KEY, quiz);

        return quiz;
    }

    // 세션에 저장되어 있는 QUIZ 반환.
    public Quiz getIssuedQuiz(HttpSession session) {
        return (Quiz) session.getAttribute(QUIZ_SESSION_KEY);
    }

    // QUIZ 정답 여부 확인
    public boolean checkAnswer(HttpSession session, String userAnswer) {
        Quiz quiz = (Quiz) session.getAttribute(QUIZ_SESSION_KEY);

        if (userAnswer == null || quiz == null) {
            return false;
        }

        String correctAnswer = quiz.getCorrectAnswer().trim().toLowerCase();
        String cleanUserAnswer = userAnswer.trim().toLowerCase();

        return correctAnswer.equals(cleanUserAnswer);
    }
}
