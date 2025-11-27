package com.promptrungame.prompt_run.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.promptrungame.prompt_run.domain.GameState;
import com.promptrungame.prompt_run.domain.Quiz;
import com.promptrungame.prompt_run.dto.ChatResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class GameService {

    private final Client geminiClient;
    private final QuizService quizService;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    private static final String GAME_SYSTEM_INSTRUCTION =
            // 1. 페르소나 및 핵심 목표 정의 (최소화)
            "당신은 'Prompt Run' 텍스트 어드벤처 게임의 베테랑 게임 마스터(GM)이다. 목표는 '고가품 확보 및 탈출'이다. 총 11턴 제한.\n" +
            "톤: 현대 범죄 도시의 진지하고 긴장감 있는 분위기를 유지한다." +
            "\n\n=== 🚨 최종 출력 포맷 (절대적 규칙) 🚨 ===\n" +

            // 2. 출력 구조의 절대적 강제 (3가지 턴 유형)
            "모든 응답은 아래 3가지 유형 중 하나만 따른다. 시나리오 묘사 후 질문을 던지는 경우, 해당 유형의 포맷으로 마무리해야 한다." +

            // --- A: 일반/퀴즈 준비 턴 (Turn N) ---
            "A) 일반 턴 및 퀴즈 준비 턴: 상황 묘사 (2줄) - 행동 결과 (1문장) - [OPTIONS: A. 첫 번째 | B. 두 번째 | C. 세 번째]" +

            // --- B: 퀴즈 준비 신호 (Turn N) ---
            "B) 퀴즈 준비 신호: 턴 A의 응답 포맷을 따르되, **추가적으로 [QUIZ_REQUEST] 토큰을 함께 포함**해야 한다. (예: (상황 묘사) (행동 결과) [OPTIONS: ...] [QUIZ_REQUEST])" +

            // --- C: 턴 종료 ---
            "C) 턴 종료: 시나리오 묘사 후 [RESULT: VICTORY], [RESULT: DEATH], [RESULT: TIMEOUT] 결과 토큰만 포함." +

            "\n\n=== 💡 세부 규칙 ===\n" +
            "3. 메모리 반영: 플레이어의 모든 이전 상태(HP/장비 등)를 반드시 기억하고 시나리오에 반영한다." +
            "4. 난이도/위험 배분: 턴 1~5는 탐색, 턴 6~10는 교전/도전으로 난이도를 상승시킨다." +
            "5. 퀴즈 정답 처리: 플레이어의 답변에 대해 [QUIZ_SUCCESS] 또는 [QUIZ_FAIL] 태그를 받으면, 그 결과를 시나리오에 반영한다. [QUIZ_FAIL]일 경우 HP 감소를 명시하고 부정적인 다음 상황으로 연결한다." +
            "6. **퀴즈 실행 턴 (Turn N+1) 포맷:** 시스템 명령으로 **퀴즈 문제 제시**를 요청받은 경우, **절대로 [OPTIONS: ...] 태그를 포함하지 않고** 퀴즈에 맞는 상황 묘사만 한다." +
            "7. [최종 경고] 모든 턴은 위 A, B, C 유형 중 하나로 종결되어야 한다. 이 외의 포맷은 금지한다.";

    public GameService(Client geminiClient, QuizService quizService) {
        this.geminiClient = geminiClient;
        this.quizService = quizService;
    }

    private static final String GAME_STATE_SESSION_KEY = "PromptRunState";
    private static final String QUIZ_SESSION_KEY = "PromptRunQuiz";

    public ChatResponse getResponseFromGemini(String userMessage, HttpSession session) {

        GameState state = (GameState) session.getAttribute(GAME_STATE_SESSION_KEY);

        if (state == null) {
            state = GameState.builder()
                    .theme("default")
                    .hp(100)
                    .currentTurn(1)
                    .startedAt(java.time.OffsetDateTime.now())
                    .lastUpdatedAt(java.time.OffsetDateTime.now())
                    .build();
        }

        // 퀴즈 답변 처리
        if (userMessage != null && userMessage.startsWith("[QUIZ_ANSWER")) {
            String userAnswer = userMessage.substring("[QUIZ_ANSWER]:".length()).trim();

            boolean isCorrect = quizService.checkAnswer(session, userAnswer);

            String AnsTag = isCorrect ? "[QUIZ_SUCCESS]" : "[QUIZ_FAIL]";

            if (!isCorrect) {
                // HP 감소
                state.setHp(state.getHp() - 10);
            }

            userMessage = "플레이어가 답변했습니다. 결과는" + AnsTag + "입니다. 이 결과를 바탕으로 규칙 5를 따라 진행하시오.";
            log.info("퀴즈 답변 처리: " + userMessage);

        }

        boolean shouldShowQuizUI = state.isQuizPending();
        if (shouldShowQuizUI) {
            state.setQuizPending(false);

            userMessage = "플레이어가 이전 선택을 완료했습니다. 이 결과에 따라 다음 상황을 묘사하고, **복잡한 잠금장치나 해제해야 할 보안 시스템을 만나 퀴즈를 풀어야 된다는 내용으로 마감하십시오.** 절대로 [OPTIONS: ...]나 **새로운 퀴즈 문제 자체**를 생성하지 마십시오.";

        }

        int currentTurn = state.getCurrentTurn();

        // 현재 턴이 11턴을 초과하였을 경우
        if (currentTurn > 11) {
            userMessage = "턴 제한 10회를 초과했습니다. 즉시 [RESULT: TIMEOUT] 키워드를 사용하여 스토리를 종료하고 결말을 묘사하십시오.";
            log.info("턴 제한 10회 초과 [RESULT: TIMEOUT]");
        }


        List<com.google.genai.types.Content> contents = new ArrayList<>();

        // 1. System Instruction을 Content 객체로 포장
        Content systemInstructionContent = Content.builder()
                .role("system") // 시스템 명령어임을 명시
                .parts(List.of(Part.builder().text(GAME_SYSTEM_INSTRUCTION).build()))
                .build();

        // 2. AI에게 줄 contents에 이전 대화 추가를 위해
        for (String hist : state.getHistory()) {
            contents.add(Content.builder()
                    .role("user")
                    .parts(List.of(Part.builder().text(hist).build()))
                    .build());
        }

        // 3. 사용자 메시지를 Content 객체로 생성
        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.builder().text(userMessage).build()))
                .build();
        contents.add(userContent);

        // Gemini의 System Instruction 사용(고정된 Prompt 형식)
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstructionContent)
                .temperature(0.7f) // 온도 설정해서 창의성 발휘 유도
                .maxOutputTokens(2048) // 최대 글자수
                .build();

        // Gemini 한테 userMessage 던져주고 결과 값 받아오기
        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                    modelName,
                    contents,
                    config
            );
            String responseText = response.text();

            if (responseText == null) {
                log.error("Gemini API 호출 결과 responseText가 null입니다.");
                return ChatResponse.builder()
                        .error("AI 응답이 비었습니다.")
                        .build();
            }

            // Quiz 보여줘야할 상황 타이밍을 제공
            boolean requestQuiz = responseText.contains("[QUIZ_REQUEST]");

            boolean isOptionTag = responseText.contains("[OPTIONS: ");
            boolean isGameEnded =
                    responseText.contains("[RESULT: VICTORY]") ||
                    responseText.contains("[RESULT: DEATH]") ||
                    responseText.contains("[RESULT: TIMEOUT]");

            log.warn("--- 태그 인식 상태 ---");
            log.warn("REQ_QUIZ (AI): {}", requestQuiz); // [QUIZ_REQUEST]가 감지되었는가?
            log.warn("IS_OPTION: {}", isOptionTag);     // [OPTIONS: ]가 감지되었는가?
            log.warn("IS_ENDED: {}", isGameEnded);       // [RESULT: ]가 감지되었는가?
            log.warn("--------------------");

            Quiz quizToSend = null;

            if (shouldShowQuizUI) {
                quizToSend = quizService.getIssuedQuiz(session);

                if (quizToSend == null) {
                    quizToSend = quizService.issueNewQuiz(session);
                }

                responseText = responseText.replaceAll("\\[OPTIONS:\\s*[\\s\\S]*?\\]", "").trim();
                responseText = responseText.replaceAll("\\[QUIZ_REQUEST\\]", "").trim();
            } else if (requestQuiz) {
                // AI가 QUIZ_REQUEST를 포함시켜서 반환을 잘 해줬을 때, 질문 session 저장 준비
                quizService.issueNewQuiz(session);
                state.setQuizPending(true);

                responseText = responseText.replaceAll("\\[OPTIONS:\\s*[\\s\\S]*?\\]", "").trim();
                responseText = responseText.replaceAll("\\[QUIZ_REQUEST\\]", "").trim();

                // responseText += "\n\n[OPTIONS: A. 확인 후 계속 진행 | B. 주변을 다시 확인한다]";
                responseText += "\n\n[OPTIONS: A. 문제 확인하기 ]";
            }

            // session에 상태 업데이트
            state.getHistory().add("USER: " + userMessage);
            state.getHistory().add("MODEL: " + responseText);

            // 현재 턴 증가
            state.setCurrentTurn(currentTurn + 1);
            state.setLastUpdatedAt(java.time.OffsetDateTime.now());

            // 게임 종료가 되었다면 Session 초기화
            if (isGameEnded) {
                session.removeAttribute(GAME_STATE_SESSION_KEY);
                log.info("session 이전 정보 초기화 성공");
            } else {
                session.setAttribute(GAME_STATE_SESSION_KEY, state);
                log.info("session에 이전 정보 저장 성공");
            }


            ChatResponse dto = ChatResponse.builder()
//                    .response(responseText.replaceAll("\\[QUIZ_REQUEST\\]", "").replaceAll("\\[RESULT:[^\\]]*\\]", "").trim())
                    .response(responseText.replaceAll("\\[QUIZ_REQUEST\\]", "").replaceAll("\\[OPTIONS:\\s*[\\s\\S]*?\\]", "").trim())
                    .rawResponse(responseText)
                    .isQuizRequest(shouldShowQuizUI)
                    .quizData(quizToSend)
                    .isGameEnded(isGameEnded)
                    .build();

            return dto;


        // 예외처리
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: {} ", e.getMessage(), e);
            return ChatResponse.builder()
                    .error("현재 AI 서버와 통신할 수 없습니다.")
                    .isQuizRequest(false)
                    .isGameEnded(false)
                    .build();
        }
    }
}
