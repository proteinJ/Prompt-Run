// 상수 및 상태 정의
const STORY_FEED = document.getElementById('story-feed');
const INPUT_AREA = document.getElementById('input-area');
const STATUS_HP = document.getElementById('status-hp');
const STATUS_TURNS = document.getElementById('status-turns');
const SEND_BUTTON = document.getElementById('send-button');

// 현재 게임 상태
let currentTurn = 0;
let isWaitingForQuizAnswer = false;

// ----------------------------------------------------
// I. 렌더링 및 UI 함수
// ----------------------------------------------------

/**
 * 메시지를 스토리 피드에 추가하고 스크롤을 맨 아래로 이동합니다.
 * @param {string} text - 표시할 텍스트
 * @param {string} sender - 'user', 'model', 'system' 중 하나
 */
function addMessageToFeed(text, sender = 'model') {
    const messageDiv = document.createElement('div');
    messageDiv.classList.add('message', `${sender}-message`);

    // 개행 문자를 <br> 태그로 변환하여 HTML에 맞게 표시
    messageDiv.innerHTML = text.replace(/\n/g, '<br>');

    STORY_FEED.appendChild(messageDiv);
    STORY_FEED.scrollTop = STORY_FEED.scrollHeight;
}

/**
 * 게임 상태(HP, 턴)를 업데이트합니다.
 * @param {number} hp - 현재 HP
 * @param {number} turn - 현재 턴
 */
function updateGameStatus(hp, turn) {
    STATUS_HP.textContent = `HP: ${hp}`;
    STATUS_TURNS.textContent = `턴: ${turn}`;
}

/**
 * AI 응답 텍스트에서 [OPTIONS: ...] 블록을 찾아 선택지 버튼을 렌더링합니다.
 * @param {string} responseText - AI의 응답 텍스트
 */
function renderOptions(responseText) {
    INPUT_AREA.innerHTML = '';
    const optionsMatch = responseText.match(/\[OPTIONS:\s*(.*)]/);

    if (optionsMatch && !isWaitingForQuizAnswer) {
        const optionsString = optionsMatch[1];
        const optionsArray = optionsString.split(/\s*\|\s*/);

        optionsArray.forEach(option => {
            if (option.trim()) {
                const button = document.createElement('button');
                // 예: "A. 왼쪽 골목으로 들어간다" -> 'A'만 추출하여 메시지로 사용
                const actionKey = option.trim().split('.')[0].trim();
                button.textContent = option.trim();
                button.onclick = () => sendMessage(actionKey);
                INPUT_AREA.appendChild(button);
            }
        });
    }
}

/**
 * 퀴즈 요청 시 퀴즈 입력 UI를 렌더링합니다.
 */
function renderQuizInput() {
    INPUT_AREA.innerHTML = '';

    const input = document.createElement('input');
    input.type = 'text';
    input.id = 'quiz-input';
    input.placeholder = '정답을 입력하거나 선택하세요 (예: A, 42)';

    const sendBtn = document.createElement('button');
    sendBtn.textContent = '정답 제출';
    sendBtn.onclick = () => submitQuizAnswer(document.getElementById('quiz-input').value);

    INPUT_AREA.appendChild(input);
    INPUT_AREA.appendChild(sendBtn);

    input.focus();
}

/**
 * 퀴즈 정답 제출 시 호출되며, 서버가 요구하는 태그 형식으로 메시지를 구성하여 전송합니다.
 * @param {string} answer - 사용자가 입력한 정답
 */
function submitQuizAnswer(answer) {
    if (!answer.trim()) return;

    // GameService가 요구하는 [QUIZ_ANSWER] 태그 형식으로 메시지 구성
    const taggedAnswer = `[QUIZ_ANSWER]: ${answer.trim()}`;
    isWaitingForQuizAnswer = false; // 퀴즈 대기 상태 해제
    sendMessage(taggedAnswer);
}

// ----------------------------------------------------
// II. 통신 및 메인 로직
// ----------------------------------------------------

/**
 * 서버에 메시지를 전송하고 응답을 처리하는 메인 함수입니다.
 * @param {string} [message="모험 시작"] - 사용자 메시지 (선택 키 또는 퀴즈 정답 태그)
 */
async function sendMessage(message = "모험 시작") {

    // 초기 시작 버튼 비활성화 (버튼이 메시지를 대체함)
    if (message === "모험 시작") {
        SEND_BUTTON.remove();
        // 턴 카운터는 AI 응답 후 업데이트되므로, 여기서만 초기화
        currentTurn = 1;
        updateGameStatus(100, currentTurn);
        // 모험 시작 메시지를 AI에게 보냄
        message = "게임 시작. 시나리오를 시작해 주세요.";
    }

    addMessageToFeed(message, 'user');

    try {
        const response = await fetch('/api/game/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message: message }) // ChatRequest DTO 형식
        });

        if (!response.ok) {
            new Error(`HTTP 오류: ${response.status}`);
        }

        /** @type {{response: string, rawResponse: string, isQuizRequest: boolean, isGameEnded: boolean, error: string}} */
        const data = await response.json();

        if (data.error) {
            addMessageToFeed(`오류: ${data.error}`, 'system');
            return;
        }

        // 퀴즈 정답 결과 처리 (GameService의 퀴즈 답변 처리 블록에서 오는 경우)
        if (data.response === '[QUIZ_SUCCESS]' || data.response === '[QUIZ_FAIL]') {
            // 퀴즈 결과는 history에 남지 않으므로, 다음 턴을 유도하는 메시지를 전송
            const resultMessage = data.response === '[QUIZ_SUCCESS]' ?
                "정답입니다. 다음 상황을 요청합니다." :
                "오답입니다. 상황 페널티를 반영하여 다음 상황을 요청합니다.";
            addMessageToFeed(resultMessage, 'system');

            // 퀴즈 결과에 따른 다음 턴을 AI에게 요청
            await sendMessage(resultMessage);
            return;
        }

        // ----------------------------------------------------
        // 일반적인 AI 응답 처리
        // ----------------------------------------------------

        // 1. 스토리 피드 업데이트
        addMessageToFeed(data.response, 'model');

        // 2. 상태 업데이트
        // (현재 HP와 턴 카운트 정보는 서버의 GameState DTO에서 받아와야 정확하지만,
        // 서버에서 반환하지 않으므로 임시로 턴만 증가시킵니다.)
        if (!data.isGameEnded && !data.isQuizRequest) {
            currentTurn++;
        }
        updateGameStatus(100, currentTurn); // HP는 고정값으로 가정

        // 3. 퀴즈 요청 처리
        if (data.isQuizRequest) {
            isWaitingForQuizAnswer = true;
            addMessageToFeed("❗ 퀴즈 요청: 다음 문제의 정답을 입력해 주세요.", 'system');
            // 퀴즈 입력 UI 렌더링 (DB에서 퀴즈를 가져오는 로직은 여기에 추가되어야 합니다.)
            renderQuizInput();
            return;
        }

        // 4. 선택지 렌더링 또는 게임 종료
        if (data.isGameEnded) {
            addMessageToFeed("✅ 게임 종료!", 'system');
            INPUT_AREA.innerHTML = '<button onclick="window.location.reload()">새 게임 시작</button>';
        } else {
            // [OPTIONS: ...] 파싱 및 버튼 렌더링
            renderOptions(data.response);
        }

    } catch (error) {
        console.error('게임 통신 중 오류 발생:', error);
        addMessageToFeed("통신 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", 'system');
        INPUT_AREA.innerHTML = '<button onclick="window.location.reload()">새로고침</button>';
    }
}

// 초기 UI 구성 (시작 버튼은 HTML에 이미 존재하므로 별도 함수 호출 불필요)