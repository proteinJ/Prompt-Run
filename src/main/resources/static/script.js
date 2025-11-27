// 상태 변수
let currentTurn = 0;
let isWaitingForQuizAnswer = false;

const getUI = () => ({
    storyFeed: document.getElementById('story-feed'),
    inputArea: document.getElementById('input-area'),
    statusHp: document.getElementById('status-hp'),
    statusTurn: document.getElementById('status-turns'),
    sendBtn: document.getElementById('send-button')
});

//#####################################
// 1. 렌더링 및 UI 함수
//#####################################

function addMessageToFeed(text, sender = 'model') {
    const ui = getUI();
    const messageDiv = document.createElement('div');
    messageDiv.classList.add('message', `${sender}-message`);
    messageDiv.innerHTML = text.replace(/\n/g, '<br>');

    if(ui.storyFeed) {
        ui.storyFeed.appendChild(messageDiv);
        ui.storyFeed.scrollTop = ui.storyFeed.scrollHeight;
    }
}

function updateGameStatus(hp, turn) {
    const ui = getUI();
    if(ui.statusHp) ui.statusHp.textContent = `HP: ${hp}`;
    if(ui.statusTurn) ui.statusTurn.textContent = `턴: ${turn}`;
}

function renderOptions(responseText) {
    const ui = getUI();
    if(!ui.inputArea) return;

    ui.inputArea.innerHTML = '';
    const optionsMatch = responseText.match(/\[OPTIONS:\s*([\s\S]*?)\]/);

    if (optionsMatch && !isWaitingForQuizAnswer) {
        const optionsString = optionsMatch[1];
        const optionsArray = optionsString.split(/\s*\|\s*/);

        optionsArray.forEach(option => {
            if (option.trim()) {
                const button = document.createElement('button');
                const actionKey = option.trim().split('.')[0].trim();
                button.textContent = option.trim();
                button.onclick = () => sendMessage(actionKey);
                ui.inputArea.appendChild(button);
            }
        });
    }
}

function submitQuizAnswer(answer) {
    if (!answer.trim()) return;
    const taggedAnswer = `[QUIZ_ANSWER]: ${answer.trim()}`;
    isWaitingForQuizAnswer = false;
    addMessageToFeed(answer.trim(), 'user');
    sendMessage(taggedAnswer);
}

function renderQuizInput(quizData) {
    const ui = getUI();

    // 안전장치: DOM 요소를 못 찾으면 중단
    if (!ui.inputArea) {
        console.error("DOM Error: input-area를 찾을 수 없습니다.");
        return;
    }

    ui.inputArea.innerHTML = '';

    console.log("--- [DEBUG] 퀴즈 데이터 렌더링 시작 ---", quizData);

    if (!quizData) {
        addMessageToFeed("오류: 퀴즈 데이터가 도착하지 않았습니다.", 'system');
        return;
    }

    // 1. 퀴즈 질문 표시
    addMessageToFeed(`[수수께끼 발동] ${quizData.question}`, 'system');

    // 2. 선택지 렌더링
    if (quizData.choices && quizData.choices.length > 0) {
        quizData.choices.forEach(choice => {
            const button = document.createElement('button');
            // "A. 설명" 에서 "A"만 추출
            const answerKey = choice.trim().split('.')[0].trim();

            button.textContent = choice.trim();
            button.onclick = () => submitQuizAnswer(answerKey);

            ui.inputArea.appendChild(button);
        });
    }
}


// #####################################
// 2. 메인 로직
// #####################################

async function sendMessage(message = "게임 시작. 시나리오를 시작해 주세요.") {
    const ui = getUI();

    if (message === "게임 시작. 시나리오를 시작해 주세요.") {
        if(ui.sendBtn) ui.sendBtn.remove();
        currentTurn = 1;
        updateGameStatus(100, currentTurn);
    }

    if (!message.startsWith("[QUIZ_ANSWER]:")) {
        addMessageToFeed(message, 'user');
    }

    try {
        // URL을 명시적으로 localhost:8080으로 지정 (CORS 테스트용)
        const response = await fetch('http://localhost:8080/api/game/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message })
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const data = await response.json();

        if (data.error) {
            addMessageToFeed(`오류: ${data.error}`, 'system');
            return;
        }

        addMessageToFeed(data.response, 'model');

        if (!data.gameEnded && !data.quizRequest) {
            currentTurn++;
        }
        updateGameStatus(100, currentTurn);

        // 입력창 초기화
        if (ui.inputArea) ui.inputArea.innerHTML = '';

        // 퀴즈 처리
        if (data.quizRequest) {
            isWaitingForQuizAnswer = true;

            // 데이터가 잘 왔는지 콘솔에서 확인하세요
            console.log("--- [DEBUG] 퀴즈 데이터 수신 ---", data.quizData);

            renderQuizInput(data.quizData);
            return;
        }

        if (data.gameEnded) {
            addMessageToFeed("✅ 게임 종료!", 'system');
            if(ui.inputArea) ui.inputArea.innerHTML = '<button onclick="window.location.reload()">새 게임 시작</button>';
        } else {
            renderOptions(data.rawResponse);
        }

    } catch (error) {
        console.error('통신 오류:', error);
        addMessageToFeed(`서버 통신 오류: ${error.message}`, 'system');
        if(ui.inputArea) ui.inputArea.innerHTML = '<button onclick="window.location.reload()">새로고침</button>';
    }
}

// #####################################
// 3. 초기화
// #####################################
document.addEventListener('DOMContentLoaded', () => {
    const ui = getUI();
    if (ui.sendBtn) {
        ui.sendBtn.onclick = () => sendMessage();
    }
});