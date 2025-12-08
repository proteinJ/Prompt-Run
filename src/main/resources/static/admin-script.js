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

// 페이지 로드 시 초기 실행
loadMembers();
