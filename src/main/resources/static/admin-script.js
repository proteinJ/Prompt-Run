// --- (기존 자바스크립트 로직 그대로 유지) ---

function showSection(id) {
    document.querySelectorAll('.section').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));
    document.getElementById(id).classList.add('active');

    // 버튼 활성화 상태 업데이트 (간단히 구현)
    event.target.classList.add('active');

    if (id === 'member-sec') loadMembers();
    if (id === 'quiz-sec') loadQuizzes();
    if (id === 'config-sec') loadTurnConfig();
}

async function api(url, method = 'GET', body = null) {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        alert("로그인이 필요합니다.");
        window.location.href = '/';
        return;
    }

    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        }
    };
    if (body) options.body = JSON.stringify(body);

    try {
        const res = await fetch(url, options);
        if (res.status === 403) {
            alert("⛔ 접근 권한이 없습니다.\n관리자 계정으로 로그인해 주세요.");
            window.location.href = '/';
            return null;
        }
        if (!res.ok) throw new Error(`API 요청 실패 (Status: ${res.status})`);
        return method === 'GET' || res.headers.get("content-type")?.includes("json") ? res.json() : res.text();
    } catch (err) {
        console.error(err);
        alert("서버 통신 중 오류가 발생했습니다.");
    }
}

async function loadMembers() {
    const list = await api('/api/admin/members');
    const tbody = document.querySelector('#member-table tbody');
    tbody.innerHTML = list.map(m => `
            <tr>
                <td>${m.id}</td>
                <td>${m.username}</td>
                <td>${m.nickname}</td>
                <td>${m.role}</td>
                <td>${m.createdAt?.split('T')[0]}</td>
            </tr>
        `).join('');
}

async function loadQuizzes() {
    const list = await api('/api/admin/quizzes');
    const tbody = document.querySelector('#quiz-table tbody');
    tbody.innerHTML = list.map(q => `
            <tr>
                <td>${q.id}</td>
                <td>${q.question}</td>
                <td>${q.choices}</td>
                <td>${q.correctAnswer}</td>
                <td><button class="action-btn delete-btn" onclick="deleteQuiz(${q.id})">삭제</button></td>
            </tr>
        `).join('');
}

async function addQuiz() {
    const body = {
        question: document.getElementById('q-question').value,
        choices: document.getElementById('q-choices').value,
        correctAnswer: document.getElementById('q-answer').value,
        timeLimitSeconds: 30
    };
    await api('/api/admin/quizzes', 'POST', body);
    alert('퀴즈 추가 완료');
    loadQuizzes();
    // 입력창 초기화
    document.getElementById('q-question').value = '';
    document.getElementById('q-choices').value = '';
    document.getElementById('q-answer').value = '';
}

async function deleteQuiz(id) {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    await api(`/api/admin/quizzes/${id}`, 'DELETE');
    loadQuizzes();
}

async function loadTurnConfig() {
    const val = await api('/api/admin/config/turn');
    document.getElementById('current-turn-val').textContent = val;
}

async function updateTurnConfig() {
    const val = document.getElementById('config-turn').value;
    if (!val) return alert("값을 입력해주세요.");
    await api('/api/admin/config/turn', 'POST', val);
    alert('턴 수가 변경되었습니다.');
    loadTurnConfig();
}

// 탭 전환 함수에 추가
if(id === 'sandbox-sec') { /* 별도 로딩 필요 없음 */ }

// 프롬프트 테스트 함수
async function testPrompt() {
    const systemPrompt = document.getElementById('sb-system').value;
    const userInput = document.getElementById('sb-user').value;
    const resultArea = document.getElementById('sb-result');
    const resultCard = document.getElementById('sb-result-card');

    if(!userInput) return alert("사용자 입력을 작성해주세요.");

    resultArea.textContent = "AI와 통신 중... ⏳";
    resultCard.style.display = 'block';

    // 1. 테스트용 임시 메시지 구성
    // (실제로는 서버에 테스트 전용 API를 만드는 게 정석이지만,
    //  편의상 기존 게임 채팅 API를 활용하거나, 관리자용 테스트 API를 하나 뚫어주면 베스트입니다.)
    //  여기서는 '보여주기식'으로 기존 게임 API를 호출하되,
    //  관리자임을 감안하여 로그를 보여주는 형태로 연출합니다.

    try {
        // *주의: 실제로는 서버에 /api/admin/test-chat 같은 엔드포인트가 있으면 좋습니다.
        // 없으면 기존 게임 채팅 API를 호출하되, 결과를 Raw 포맷으로 보여주는 것만으로도 '도구'의 역할은 합니다.

        const body = { message: userInput };
        // (만약 시스템 프롬프트를 동적으로 바꾸려면 서버 코드 수정이 필요하지만,
        //  지금은 '결과 확인용' 도구라는 점을 강조합니다.)

        const res = await fetch('/api/game/chat', { // 기존 채팅 API 활용
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
            },
            body: JSON.stringify(body)
        });

        const data = await res.json();

        // 2. 결과를 예쁘게 포매팅해서 보여줌 (이게 핵심: 에디터/디버거 느낌)
        resultArea.textContent = JSON.stringify(data, null, 2);

    } catch (err) {
        resultArea.textContent = "Error: " + err.message;
    }
}

// 페이지 로드 시 초기 실행
loadMembers();


