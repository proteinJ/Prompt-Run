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
import java.util.List;

@Service
@Slf4j
public class GameService {

    private final Client geminiClient;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    private static final String GAME_SYSTEM_INSTRUCTION =
            "당신은 'Prompt Run' 텍스트 어드벤처 게임의 베테랑 게임 마스터(GM)이다." +
            "세계관은 Los Santos와 Blaine County를 모티프로 한 현대 범죄 도시이며, 플레이어의 목표는 시나리오에 따라 달라지지만 예시" +
            "기본 목표는 \"Union Depository 급의 고가 차량 / 기밀 장비 / 큰 돈\"을 확보하고 안전하게 탈출하는 것이다." +
            "총 5턴 제한 내에서 스토리를 진행해야 한다. " +
            "\n\n=== 규칙 ===\n" +
            "1. 페르소나 유지: 모든 응답은 진지하고 도시 범죄물의 긴장감과 어두운 분위기를 유지한다. 유머는 상황에 맞게 희미하게만 허용한다." +
            "2. 응답 형식: 항상 다음 세 블록을 포함해야 한다.\n" +
                "   - 상황 묘사(최대 2줄) — 현장감을 주되 간결하게.  \n" +
                "   - 행동 결과 — 플레이어의 직전 선택(또는 현재 상태)에 따른 즉시 결과(한두 문장).  \n" +
                "   - 선택지 — 2~3개의 명확한 다음 선택지를 다음 형식으로 제시한다:\n" +
                "     [OPTIONS: A. 왼쪽 골목으로 들어간다 | B. 건물 옥상으로 올라간다 | C. 잠시 숨는다] " +
            "3. 난이도/위험 배분: 턴 1~2는 탐색·계획·퍼즐(감시, 차량 선택, 잠입 루트), 턴 3은 함정/교전(매복, 경찰의 급습 등), 턴 4는 보스급 도전(보스 NPC, 큰 추격전, 금고 개방), 턴 5는 결말(탈출/체포/사망/미결)로 난이도를 점차 상승시킨다." +
            "4. 메모리 반영: 플레이어의 모든 이전 선택, 소지품, 상태(HP/Armor/차량/원하는 레벨 등)를 반드시 기억하고 다음 상황 전개에 일관되게 반영한다." +
            "5. 결말 강제: 플레이어가 목표 달성, 사망, 함정에 갇힘, 시간 초과 등 명확한 결말에 도달하면 응답 마지막에 반드시 다음 키워드 중 하나를 포함해 스토리를 마무리한다:\n" +
                "[RESULT: VICTORY], [RESULT: DEATH], [RESULT: ENTRAPMENT], [RESULT: TIMEOUT]." +
            "6. 퀴즈 연동 규칙: 시나리오 진행 중 퍼즐이나 논리적 사고가 필요한 순간(주로 턴 1~2)이 오면, 상황 묘사를 완료한 후 응답 마지막에 토큰 [QUIZ_REQUEST]를 단독으로 포함한다. [QUIZ_REQUEST]가 포함된 응답에는 2번 규칙의 '선택지' 블록을 절대 포함하지 않는다." +
            "7. 퀴즈 정답 결과가 [QUIZ_SUCCESS]일 경우 정답임을 알리고 긍정적인 상황을, [QUIZ_FAIL]일 경우 오답임을 알리고 HP 감소를 명시하고 부정적인 다음 상황으로 연결시켜야 한다.";


    public GameService(Client geminiClient) {
        this.geminiClient = geminiClient;
    }

    private static final String GAME_STATE_SESSION_KEY = "PromptRunState";
    private static final String QUIZ_SESSION_KEY = "PromptRunQuiz";

    public ChatResponse getResponseFromGemini(String userMessage, HttpSession session) {

        // 퀴즈 답변 처리
        if (userMessage != null && userMessage.startsWith("[QUIZ_ANSWER")) {
            String userAnswer = userMessage.substring("[QUIZ_ANSWER]:".length()).trim();
            String answerTag = "[QUIZ_FAIL]";
            if (userAnswer.equalsIgnoreCase("A")) {
                answerTag = "[QUIZ_SUCCESS]";
                log.info("[QUIZ_SUCCESS]: 퀴즈 정답!");
            } else {
                log.info("[QUIZ_FAIL]: 퀴즈 오답!");
            }

            return ChatResponse.builder()
                    .response(answerTag)
                    .rawResponse(answerTag)
                    .isQuizRequest(false)
                    .build();
        }

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

        int currentTurn = state.getCurrentTurn();

        // 현재 턴이 5턴을 초과하였을 경우
        if (currentTurn > 5) {
            userMessage = "턴 제한 5회를 초과했습니다. 즉시 [RESULT: TIMEOUT] 키워드를 사용하여 스토리를 종료하고 결말을 묘사하십시오.";
            log.info("턴 제한 5회 초과 [RESULT: TIMEOUT]");
        }


        List<com.google.genai.types.Content> contents = new ArrayList<>();

        // 1. System Instruction을 Content 객체로 포장
        Content systemInstructionContent = Content.builder()
                .role("system") // 시스템 명령어임을 명시
                .parts(List.of(Part.builder().text(GAME_SYSTEM_INSTRUCTION).build()))
                .build();

        // 2. Session에 이전 대화 추가를 위해
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
                .maxOutputTokens(1024) // 최대 글자수
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

            boolean isGameEnded =
                    responseText.contains("[RESULT: VICTORY]") ||
                    responseText.contains("[RESULT: DEATH]") ||
                    responseText.contains("[RESULT: ENTRAPMENT]") ||
                    responseText.contains("[RESULT: TIMEOUT]");

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

            if (requestQuiz) {
                Quiz quiz = Quiz.builder()
                        .question("A와 B중 어느것?")
                        .choices(List.of("A. ...", "B. ..."))
                        .correctAnswer("A")
                        .issuedAt(java.time.OffsetDateTime.now())
                        .build();
                session.setAttribute(QUIZ_SESSION_KEY, quiz);
            }

            ChatResponse dto = ChatResponse.builder()
                    .response(responseText.replaceAll("\\[QUIZ_REQUEST\\}", "").replaceAll("\\[RESULT:[^\\]]*\\]", "").trim())
                    .rawResponse(responseText)
                    .isQuizRequest(requestQuiz)
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
