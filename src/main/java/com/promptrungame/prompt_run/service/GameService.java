package com.promptrungame.prompt_run.service;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.promptrungame.prompt_run.domain.GameConfig;
import com.promptrungame.prompt_run.domain.GameState;
import com.promptrungame.prompt_run.dto.ChatResponse;
import com.promptrungame.prompt_run.dto.GameRecordRequest;
import com.promptrungame.prompt_run.dto.Quiz; // ✅ DTO Import 확인
import com.promptrungame.prompt_run.repository.GameConfigRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GameService {

    // 1. 인터페이스 주입 (Mock 또는 Real이 들어옴)
    private final GeminiClientService geminiClientService;
    private final QuizService quizService;
    private final GameRecordService gameRecordService;
    private final GameConfigRepository gameConfigRepository;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    // 시스템 프롬프트
    private static final String GAME_SYSTEM_INSTRUCTION =
            "당신은 'Prompt Run' 텍스트 어드벤처 게임의 베테랑 게임 마스터(GM)이다. 목표는 '고가품 확보 및 탈출'이다.\n" +
                    "톤: 현대 범죄 도시의 진지하고 긴장감 있는 분위기를 유지한다." +
                    "\n\n=== 최종 출력 포맷 (절대적 규칙) ===\n" +
                    "모든 응답은 아래 3가지 유형 중 하나만 따른다. 시나리오 묘사 후 질문을 던지는 경우, 해당 유형의 포맷으로 마무리해야 한다." +
                    "A) 일반 턴 및 퀴즈 준비 턴: 상황 묘사 (2줄) - 행동 결과 (1문장) - [OPTIONS: A. 첫 번째 | B. 두 번째 | C. 세 번째]" +
                    "B) 퀴즈 준비 신호: 턴 A의 응답 포맷을 따르되, **추가적으로 [QUIZ_REQUEST] 토큰을 함께 포함**해야 한다. (예: (상황 묘사) (행동 결과) [OPTIONS: ...] [QUIZ_REQUEST])" +
                    "C) 턴 종료: 시나리오 묘사 후 [RESULT: VICTORY], [RESULT: DEATH], [RESULT: TIMEOUT] 결과 토큰만 포함." +
                    "\n\n=== 세부 규칙 ===\n" +
                    "3. 메모리 반영: 플레이어의 모든 이전 상태(HP/장비 등)를 반드시 기억하고 시나리오에 반영한다." +
                    "5. 퀴즈 정답 처리: 플레이어의 답변에 대해 [QUIZ_SUCCESS] 또는 [QUIZ_FAIL] 태그를 받으면, 그 결과를 시나리오에 반영한다. [QUIZ_FAIL]일 경우 HP 감소를 명시하고 부정적인 다음 상황으로 연결한다." +
                    "6. **퀴즈 실행 턴 (Turn N+1) 포맷:** 시스템 명령으로 **퀴즈 문제 제시**를 요청받은 경우, **절대로 [OPTIONS: ...] 태그를 포함하지 않고** 퀴즈에 맞는 상황 묘사만 한다." +
                    "7. [최종 경고] 모든 턴은 위 A, B, C 유형 중 하나로 종결되어야 한다. 이 외의 포맷은 금지한다.";

    // 생성자 주입
    public GameService(GeminiClientService geminiClientService, QuizService quizService, GameRecordService gameRecordService, GameConfigRepository gameConfigRepository) {
        this.geminiClientService = geminiClientService;
        this.quizService = quizService;
        this.gameRecordService = gameRecordService;
        this.gameConfigRepository = gameConfigRepository;
    }

    private static final String GAME_STATE_SESSION_KEY = "PromptRunState";
    private static final String QUIZ_SESSION_KEY = "PromptRunQuiz";

    public ChatResponse getResponseFromGemini(String userMessage, HttpSession session, Long memberId) {

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
                state.setHp(state.getHp() - 10);
            }
            userMessage = "플레이어가 답변했습니다. 결과는" + AnsTag + "입니다. 이 결과를 바탕으로 규칙 5를 따라 진행하시오.";
            log.info("퀴즈 답변 처리: " + AnsTag);
        }

        boolean shouldShowQuizUI = state.isQuizPending();
        if (shouldShowQuizUI) {
            state.setQuizPending(false);
            userMessage = "플레이어가 이전 선택을 완료했습니다. 이 결과에 따라 다음 상황을 묘사하고, **복잡한 잠금장치나 해제해야 할 보안 시스템을 만나 퀴즈를 풀어야 된다는 내용으로 마감하십시오.** 절대로 [OPTIONS: ...]나 **새로운 퀴즈 문제 자체**를 생성하지 마십시오.";
        }


        // #### Max_Turn Limit 가져오기 ####
        int currentTurn = state.getCurrentTurn();
        int maxTurnLimit = 11; // 기본값 11턴

        try {
            maxTurnLimit = Integer.parseInt(
                    gameConfigRepository.findById("MAX_TURN")
                            .map(GameConfig::getConfigValue)
                            .orElse("11")
            );
        } catch (Exception e) {
            log.error("Max Turn 가져오기 실패, 기본값 11턴 사용");
        }

        if (currentTurn > 11) {
            userMessage = "턴 제한 11회를 초과했습니다. 즉시 [RESULT: TIMEOUT] 키워드를 사용하여 스토리를 종료하고 결말을 묘사하십시오.";
            log.info("턴 제한 초과 [RESULT: TIMEOUT]");
        }

        // --- Content 조립  ---
        List<Content> contents = new ArrayList<>();

        // 1. System Instruction
        Content systemInstructionContent = Content.builder()
                .role("system")
                .parts(List.of(Part.builder().text(GAME_SYSTEM_INSTRUCTION).build()))
                .build();

        // 2. History
        for (String hist : state.getHistory()) {
            contents.add(Content.builder()
                    .role("user")
                    .parts(List.of(Part.builder().text(hist).build()))
                    .build());
        }

        // 3. User Message
        contents.add(Content.builder()
                .role("user")
                .parts(List.of(Part.builder().text(userMessage).build()))
                .build());

        // Config 설정
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstructionContent)
                .temperature(0.7f)
                .maxOutputTokens(2048)
                .build();

        // 2. Gemini API 호출 (인터페이스 사용!)
        try {
            // RealGeminiClientService가 있으면 구글로 가고, Mock이면 가짜 응답이 옴.
            String responseText = geminiClientService.generateContent(
                    modelName,
                    contents,
                    config
            );

            if (responseText == null) {
                log.error("Gemini API 호출 결과 responseText가 null입니다.");
                return ChatResponse.builder().error("AI 응답이 비었습니다.").build();
            }

            // --- 응답 처리 로직 ---
            boolean requestQuiz = responseText.contains("[QUIZ_REQUEST]");
            boolean isGameEnded = responseText.contains("[RESULT:");

            log.info("--- TURN: {} 태그 인식 상태 ---", currentTurn);
            log.info("REQ_QUIZ: {}, ENDED: {}", requestQuiz, isGameEnded);

            // ✅ QuizEntity -> Quiz (DTO) 사용
            Quiz quizDtoToSend = null;

            if (shouldShowQuizUI) {
                // 퀴즈 출제 (DB -> DTO)
                quizDtoToSend = quizService.getStoredQuiz(session);

                responseText = responseText.replaceAll("\\[OPTIONS:\\s*[\\s\\S]*?\\]", "").trim();
                responseText = responseText.replaceAll("\\[QUIZ_REQUEST\\]", "").trim();

            } else if (requestQuiz) {
                quizService.issueNewQuiz(session);
                // 퀴즈 예약
                state.setQuizPending(true);

                responseText = responseText.replaceAll("\\[OPTIONS:\\s*[\\s\\S]*?\\]", "").trim();
                responseText = responseText.replaceAll("\\[QUIZ_REQUEST\\]", "").trim();
                responseText += "\n\n[OPTIONS: A. 문제 확인하기 ]";

                log.info("📢 퀴즈 감지됨 -> DB 조회 및 세션 저장 완료. 다음 턴에 표시 대기.");
            }

            state.getHistory().add("USER: " + userMessage);
            state.getHistory().add("MODEL: " + responseText);
            state.setCurrentTurn(currentTurn + 1);
            state.setLastUpdatedAt(java.time.OffsetDateTime.now());

            if (isGameEnded) {
                String endResultTag = "UNKNOWN";
                if (responseText.contains("VICTORY")) endResultTag = "VICTORY";
                else if (responseText.contains("DEATH")) endResultTag = "DEATH";
                else if (responseText.contains("TIMEOUT")) endResultTag = "TIMEOUT";

                if (memberId == null) {
                    log.warn("🚨 Member ID 누락. 기록 저장 불가.");
                } else {
                    String fullHistory = String.join("\n\n--- TURN SEPARATOR ---\n\n", state.getHistory());

                    GameRecordRequest finalRecord = GameRecordRequest.builder()
                            .hp(state.getHp())
                            .promptUsed(state.getTheme())
                            .attemptCount(state.getCurrentTurn() - 1)
                            .isSuccess(responseText.contains("[RESULT: VICTORY]"))
                            .end_result(endResultTag)
                            .playedAt(state.getStartedAt().toLocalDateTime())
                            .fullConversationHistory(fullHistory)
                            .build();

                    gameRecordService.saveGameRecord(memberId, finalRecord);
                    log.info("✅ 게임 기록 저장 성공");
                }

                session.removeAttribute(GAME_STATE_SESSION_KEY);
                session.removeAttribute(QUIZ_SESSION_KEY);
            } else {
                session.setAttribute(GAME_STATE_SESSION_KEY, state);
            }

            return ChatResponse.builder()
                    .response(responseText)
                    .rawResponse(responseText)
                    .isQuizRequest(shouldShowQuizUI)
                    .quizEntityData(quizDtoToSend) // DTO 필드명 (quizData)
                    .isGameEnded(isGameEnded)
                    .build();

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: {} ", e.getMessage(), e);
            state.setCurrentTurn(currentTurn - 1); // 턴 롤백
            return ChatResponse.builder()
                    .error("현재 AI 서버와 통신할 수 없습니다: " + e.getMessage())
                    .isQuizRequest(false)
                    .isGameEnded(false)
                    .build();
        }
    }
}