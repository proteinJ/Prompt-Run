package com.promptrungame.prompt_run.service;

import com.promptrungame.prompt_run.domain.QuizEntity;
import com.promptrungame.prompt_run.dto.Quiz;
import com.promptrungame.prompt_run.repository.QuizRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private static final String QUIZ_SESSION_KEY = "PromptRunQuiz";

    /**
     * [퀴즈 출제 & 저장] - Turn N에서 미리 호출
     * DB에서 뽑아서 세션에 저장해둠.
     */
    @Transactional(readOnly = true)
    public Quiz issueNewQuiz(HttpSession session) {
        QuizEntity entity = quizRepository.findRandomQuiz()
                .orElseThrow(() -> new IllegalArgumentException("퀴즈가 없습니다."));

        session.setAttribute(QUIZ_SESSION_KEY, entity);
        log.info("✅ 퀴즈 DB 조회 및 세션 저장 완료: {}", entity.getQuestion());

        return Quiz.from(entity);
    }

    /**
     * [저장된 퀴즈 가져오기] - Turn N+1에서 호출 (★ 이 메서드 추가!)
     * 새로 뽑지 않고, 아까 저장해둔 걸 DTO로 변환만 해서 줌.
     */
    public Quiz getStoredQuiz(HttpSession session) {
        QuizEntity entity = (QuizEntity) session.getAttribute(QUIZ_SESSION_KEY);
        if (entity == null) {
            log.warn("세션에 저장된 퀴즈가 없습니다. 새로 발급합니다.");
            return issueNewQuiz(session); // 없으면 비상용으로 새로 발급
        }
        return Quiz.from(entity);
    }

    /**
     * [정답 확인]
     */
    public boolean checkAnswer(HttpSession session, String userAnswer) {
        QuizEntity quizEntity = (QuizEntity) session.getAttribute(QUIZ_SESSION_KEY);

        if (userAnswer == null || quizEntity == null) return false;

        // 1. DB 정답 (저장된 번호만 가져옴): 예: "2"
        String dbCorrectKey = quizEntity.getCorrectAnswer().trim();

        // 2. 유저 입력에서 번호만 추출 (JS에서 "2. 3개"를 보냈더라도 "2"만 남음)
        String userKey = userAnswer.split("\\.")[0].trim();

        // 3. 문자열 대 문자열 비교 (대소문자/공백 처리 필요 없음)
        return dbCorrectKey.equals(userKey);
    }
}