package com.promptrungame.prompt_run.service;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@Profile("mock")
@Slf4j
public class MockGeminiClientService implements GeminiClientService {

    private final Random random = new Random();

    @Override
    public String generateContent(String modelName, List<Content> contents, GenerateContentConfig config) {
        log.warn("🚨 [MOCK MODE] 가짜 응답 생성 로직 시작");

        String lastUserMessage = "";

        if (contents != null && !contents.isEmpty()) {
            Content lastContent = contents.get(contents.size() - 1);
            List<Part> parts = lastContent.parts().orElse(Collections.emptyList());
            if (!parts.isEmpty()) {
                String role = lastContent.role().orElse("");
                if ("user".equals(role)) {
                    lastUserMessage = parts.get(0).text().orElse("").trim();
                }
            }
        }

        log.info("📢 Mock이 받은 사용자 메시지: [{}]", lastUserMessage);

        // =========================================================
        // 1. 게임 강제 종료 (TIMEOUT)
        // =========================================================
        if (lastUserMessage.contains("턴 제한") || lastUserMessage.contains("TIMEOUT")) {
            return "[MOCK] 삐오삐오 경찰차 소리가 들린다.\n\n[RESULT: TIMEOUT]";
        }

        // =========================================================
        // 2. ★ [순서 변경] 퀴즈 문제 확인 단계 (시스템 명령) ★
        // GameService가 "퀴즈를 풀어야 된다"라고 지시하면, 여기를 먼저 타야 함!
        // =========================================================
        if (lastUserMessage.contains("퀴즈를 풀어야 된다") || lastUserMessage.contains("상황 묘사만 하고")) {
            log.info("✅ 퀴즈 UI 표시 단계 감지됨 -> 문제 묘사 텍스트 반환");
            return "[MOCK] 눈앞에 복잡한 장치가 보인다. 화면에 문제가 출력되었다.\n(제한 시간 내에 정답을 입력하라)";
        }

        // =========================================================
        // 3. 퀴즈 발동 조건 (사용자 입력)
        // =========================================================
        // 위에서 먼저 걸러지지 않으면 여기서 "퀴즈" 단어 때문에 잘못 잡힘
        if (lastUserMessage.equalsIgnoreCase("C") ||
                lastUserMessage.startsWith("C.") ||
                lastUserMessage.contains("탐색") ||
                lastUserMessage.contains("퀴즈") ||
                lastUserMessage.contains("문제")) {

            log.info("✅ 퀴즈 트리거 감지됨! [QUIZ_REQUEST] 반환");
            return "[MOCK] (두둥) 낡은 금고를 발견했다. 암호를 풀어야 한다.\n\n" +
                    "문제를 풀겠는가? [QUIZ_REQUEST]";
        }

        // =========================================================
        // 4. 퀴즈 정답/오답
        // =========================================================
        if (lastUserMessage.contains("QUIZ_SUCCESS")) {
            return "[MOCK] 정답! 문이 열렸다.\n\n[OPTIONS: A. 챙긴다 | B. 나간다]";
        }
        if (lastUserMessage.contains("QUIZ_FAIL")) {
            return "[MOCK] 땡! 함정이 발동했다.\n\n[OPTIONS: A. 도망 | B. 버틴다]";
        }

        // =========================================================
        // 5. 게임 시작
        // =========================================================
        if (lastUserMessage.contains("시나리오") || lastUserMessage.contains("시작")) {
            return "[MOCK] 게임 시작. 뒷골목이다.\n\n[OPTIONS: A. 전진 | B. 대기 | C. 탐색(퀴즈)]";
        }

        // 기본 응답
        return "[MOCK] 당신은 '" + lastUserMessage + "' 행동을 했다.\n\n다음은?\n\n[OPTIONS: A. 전진 | B. 대기 | C. 탐색(퀴즈)]";
    }
}