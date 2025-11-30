// =====================================
// script.js (수정)
// =====================================

// 상태 변수
let currentTurn = 0;
let isWaitingForQuizAnswer = false;

// 🔑 메인/게임 화면 전환을 위한 요소 추가
const getUI = () => ({
    mainScreen: document.getElementById('main-screen'),
    gameScreen: document.getElementById('game-screen'),
    storyFeed: document.getElementById('story-feed'),
    inputArea: document.getElementById('input-area'),
    statusHp: document.getElementById('status-hp'),
    statusTurn: document.getElementById('status-turns'),
    sendBtn: document.getElementById('start-button') // ID 변경
});

//#####################################
// 1. 렌더링 및 UI 함수
//#####################################

// 로그인 상태에 따른 UI 업데이트
function updateAuthStatus(isLoggedIn) {
    const menuButtons = document.getElementById('menu-buttons');
    if (!menuButtons) return;

    if (isLoggedIn) {
        menuButtons.innerHTML = '<button class="option-button" onclick="logout()">로그아웃</button>' ;
    } else {
        menuButtons.innerHTML = `
            <button class="option-button" onclick="showScreen('login-screen')">로그인</button>
            <button class="option-button" onclick="showScreen('signup-screen')">회원가입</button>
        `;
    }
}


function addMessageToFeed(text, sender = 'model') {
    const ui = getUI();
    const messageDiv = document.createElement('div');
    messageDiv.classList.add('message', `${sender}-message`);
    messageDiv.innerHTML = text.replace(/\n/g, '<br>');

    if(ui.storyFeed) {
        ui.storyFeed.innerHTML = '';
        ui.storyFeed.appendChild(messageDiv);
        ui.storyFeed.scrollTop = ui.storyFeed.scrollHeight;
    }
}

function updateGameStatus(hp, turn) {
    const ui = getUI();
    if(ui.statusHp) ui.statusHp.textContent = `HP: ${hp}`;
    if(ui.statusTurn) ui.statusTurn.textContent = `턴: ${turn}`;
}

// 🔑 옵션 렌더링 로직 (선택지는 질문 바로 아래에 현재거만 보이게)
function renderOptions(responseText) {
    const ui = getUI();
    if(!ui.inputArea) return;

    // 🔑 기존 내용 비우기 (누적되지 않도록)
    ui.inputArea.innerHTML = '';
    const optionsMatch = responseText.match(/\[OPTIONS:\s*([\s\S]*?)\]/);

    if (optionsMatch && !isWaitingForQuizAnswer) {
        const optionsString = optionsMatch[1];
        const optionsArray = optionsString.split(/\s*\|\s*/);

        const optionsContainer = document.createElement('div');
        optionsContainer.classList.add('options-container');

        optionsArray.forEach(option => {
            if (option.trim()) {
                const button = document.createElement('button');
                const actionKey = option.trim().split('.')[0].trim();
                button.textContent = option.trim();
                button.classList.add('option-button');
                button.onclick = () => sendMessage(actionKey);
                optionsContainer.appendChild(button);
            }
        });
        ui.inputArea.appendChild(optionsContainer);
    }
}

function submitQuizAnswer(answer) {
    if (!answer.trim()) return;
    const taggedAnswer = `[QUIZ_ANSWER]: ${answer.trim()}`;
    isWaitingForQuizAnswer = false;
    addMessageToFeed(answer.trim(), 'user');
    sendMessage(taggedAnswer);
}

// 🔑 퀴즈 입력 렌더링 로직 (선택지는 질문 바로 아래에 현재거만 보이게)
function renderQuizInput(quizData) {
    const ui = getUI();
    if (!ui.inputArea) {
        console.error("DOM Error: input-area를 찾을 수 없습니다.");
        return;
    }

    // 🔑 기존 내용 비우기 (누적되지 않도록)
    ui.inputArea.innerHTML = '';

    console.log("--- [DEBUG] 퀴즈 데이터 렌더링 시작 ---", quizData);

    if (!quizData) {
        addMessageToFeed("오류: 퀴즈 데이터가 도착하지 않았습니다.", 'system');
        return;
    }

    // 1. 퀴즈 질문 표시
    addMessageToFeed(`[수수께끼 발동] ${quizData.question}`, 'system');

    // 2. 선택지 렌더링
    const choicesContainer = document.createElement('div');
    choicesContainer.classList.add('options-container');

    if (quizData.choices && quizData.choices.length > 0) {
        quizData.choices.forEach(choice => {
            const button = document.createElement('button');
            const answerKey = choice.trim().split('.')[0].trim();

            button.textContent = choice.trim();
            button.classList.add('option-button');
            button.onclick = () => submitQuizAnswer(answerKey);

            choicesContainer.appendChild(button);
        });
    }
    ui.inputArea.appendChild(choicesContainer);
}


// #####################################
// 2. 메인 로직
// #####################################

// 🔑 화면 전환 로직 추가

// 🔑 메인/로그인/회원가입 화면 전환을 위한 함수 추가
function showScreen(screenId) {
    const screens = ['main-screen', 'game-screen', 'login-screen', 'signup-screen'];

    screens.forEach(id => {
        const screen = document.getElementById(id);
        if (screen) {
            if (id === screenId) {
                // 선택된 화면만 보이게
                screen.style.display = (id === 'game-screen' || id === 'main-screen') ? 'flex' : 'flex';
            } else {
                // 나머지 화면은 숨김
                screen.style.display = 'none';
            }
        }
    });
}

function startGame() {
    showScreen('game-screen'); // 게임 화면 표시

    // 게임 시작 메시지 전송 (기존 로직 유지)
    sendMessage("게임 시작. 시나리오를 시작해 주세요.");
}


async function sendMessage(message = "게임 시작. 시나리오를 시작해 주세요.") {
    const ui = getUI();

    if (message === "게임 시작. 시나리오를 시작해 주세요.") {
        currentTurn = 1;
        updateGameStatus(100, currentTurn);
        // 🔑 startGame()에서 이미 버튼이 제거되었으므로 여기서는 제거 로직 생략
    }

    if (!message.startsWith("[QUIZ_ANSWER]:")) {
        addMessageToFeed(message, 'user');
    }

    try {
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

        // 퀴즈 처리
        if (data.quizRequest) {
            isWaitingForQuizAnswer = true;
            console.log("--- [DEBUG] 퀴즈 데이터 수신 ---", data.quizData);
            renderQuizInput(data.quizData);
            return;
        }

        if (data.gameEnded) {
            addMessageToFeed("✅ 게임 종료!", 'system');
            if(ui.inputArea) ui.inputArea.innerHTML = '<button class="option-button" onclick="window.location.reload()">새 게임 시작</button>';
        } else {
            renderOptions(data.rawResponse);
        }

    } catch (error) {
        console.error('통신 오류:', error);
        addMessageToFeed(`서버 통신 오류: ${error.message}`, 'system');
        if(ui.inputArea) ui.inputArea.innerHTML = '<button class="option-button" onclick="window.location.reload()">새로고침</button>';
    }
}

// ##################
// 로그인 관련
// ###############

function getAccessToken() {
    return localStorage.getItem('accessToken');
}

async function login(username, password) {
    try {
        const response = await fetch('http://localhost:8080/api/member/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username, password: password })
        });

        if (!response.ok) {
            // HTTP 상태 코드가 4xx나 5xx 일 경우 즉시 에러 처리
            const errorData = await response.json();
            const errorMessage = errorData.message || `로그인 서버 오류: HTTP ${response.status}`;
            alert(errorMessage);
            return;
        }

        const data = await response.json();

        if (data.accessToken) { // 🔑 성공 시 처리


            localStorage.setItem('accessToken', data.accessToken);
            updateAuthStatus(true);

            alert("로그인 성공! 환영합니다.");
            showScreen('main-screen'); // 로그인 성공 후 메인 화면으로 돌아가기

            console.log("로그인 응답 데이터:", data);

        } else { // 🔑 실패 시 처리 (HTTP 200 OK지만 success: false이거나 HTTP 오류 코드)
            const errorMessage = data.message || "로그인은 성공했지만 토큰을 받지 못했습니다. 서버 설정을 확인하세요.";
            alert("로그인은 성공했지만 토큰을 받지 못했습니다. 서버 설정을 확인하세요.");
        }

    } catch (error) {
        console.error('로그인 통신 오류:', error);
        alert("서버 통신 중 오류가 발생했습니다.");
    }
}


// 로그아웃 함수
function logout() {
    // 1. 토큰 제거
    localStorage.removeItem('accessToken');
    // 2. UI 업데이트
    updateAuthStatus(false);
    // 3. 메인 화면으로 이동
    showScreen('main-screen');
    alert("로그아웃 되었습니다.");
}


// #####################################
// 3. 초기화
// #####################################
document.addEventListener('DOMContentLoaded', () => {
    const startButton = document.getElementById('start-button');
    if (startButton) {
        startButton.onclick = startGame; // 🔑 버튼 클릭 시 startGame 호출
    }

    const token = getAccessToken();
    updateAuthStatus(!!token);
    // 🔑 초기 화면은 main-screen만 보이게 설정
    showScreen('main-screen');

    // 🔑 폼 제출 이벤트 리스너 추가 (실제 백엔드 통신은 여기서 구현)
    document.getElementById('login-form')?.addEventListener('submit', (e) => {
        e.preventDefault();
        console.log("로그인 시도");

        const usernameInput = document.getElementById('login-username');
        const passwordInput = document.getElementById('login-password');

        if (usernameInput && passwordInput) {
            login(usernameInput.value, passwordInput.value); // 완성된 login 함수 호출
        } else {
            console.error("로그인 폼 요소를 찾을 수 없습니다.");
        }

    });

    document.getElementById('signup-form')?.addEventListener('submit', (e) => {
        e.preventDefault();
        console.log("회원가입 시도");
        alert("회원가입 기능 구현 예정");
        // 실제 회원가입 API 호출 로직 구현...
    });
});