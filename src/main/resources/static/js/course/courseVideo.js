/* =========================================
   1. 전역 변수 및 초기화
   ========================================= */
const videoInput = document.getElementById('video-url');
const courseInput = document.getElementById('course-id');
const chapterInput = document.getElementById('chapter-id');

const dbVideoUrl = videoInput ? videoInput.value : null;
const currentCourseId = courseInput ? courseInput.value : null;
// const currentChapterId = ... (아래에서 유동적으로 처리하기 위해 const 제거)
let currentChapterId = chapterInput ? chapterInput.value : null;

let monacoEditor = null;
let player = null;

// 퀴즈 관련 상태 변수 (중복 선언 방지용 통합)
let quizData = null;
let userAnswers = [];
let currentQIndex = 0;

/* =========================================
   2. 유튜브 플레이어 로직
   ========================================= */
function getVideoId(url) {
    if (!url || url === 'QUIZ') return null; // 퀴즈일 경우 null 반환
    try {
        const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
        const match = url.match(regExp);
        if (match && match[2].length === 11) return match[2];
        const urlObj = new URL(url);
        return urlObj.searchParams.get("v");
    } catch (e) {
        // console.error("URL 파싱 실패:", e);
        return null;
    }
}

const currentVideoId = getVideoId(dbVideoUrl);
const storageKey = "yt-time-" + (currentVideoId || "default");
let savedTime = 0;

try {
    const time = localStorage.getItem(storageKey);
    savedTime = (time && !isNaN(time)) ? Number(time) : 0;
} catch (e) {
    savedTime = 0;
}

// 유튜브 API 로드 시 실행
window.onYouTubeIframeAPIReady = function() {
    if (!currentVideoId) return; // 비디오 ID가 없으면(퀴즈 등) 생성 안 함

    player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        videoId: currentVideoId,
        playerVars: {
            'start': savedTime,
            'rel': 0,
            'autoplay': 0
        },
        events: {
            'onReady': onPlayerReady,
            'onStateChange': onPlayerStateChange
        }
    });
};

function onPlayerReady(event) {
    if(player && player.getDuration) {
        const duration = Math.floor(player.getDuration());
        if (duration > 0) saveDurationToServer(duration);
    }
    if(savedTime > 0) player.seekTo(savedTime);
}

function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.PAUSED) {
        const currentTime = Math.floor(player.getCurrentTime());
        saveCurrentTime(currentTime);
        saveProgressToServer(currentTime);
    }
    if (event.data === YT.PlayerState.ENDED) {
        localStorage.removeItem(storageKey);
    }
}

function saveCurrentTime(time) {
    if(time === undefined && player && typeof player.getCurrentTime === 'function') {
        time = Math.floor(player.getCurrentTime());
    }
    if (time !== undefined) localStorage.setItem(storageKey, time);
}

function saveProgressToServer(time) {
    if (!currentCourseId || !currentChapterId) return;
    // 퀴즈 챕터일 때는 진도율 저장 스킵 (퀴즈는 제출 시 처리)
    if (dbVideoUrl === 'QUIZ') return;

    const payload = { playTime: time };
    const url = `/course/log?courseId=${currentCourseId}&chapterId=${currentChapterId}`;

    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        keepalive: true
    }).catch(error => console.error("통신 에러:", error));
}

function saveDurationToServer(duration) {
    if (!currentChapterId || dbVideoUrl === 'QUIZ') return;
    const url = `/course/log/duration?chapterId=${currentChapterId}&duration=${duration}`;
    fetch(url, { method: 'POST' }).catch(error => console.error("영상 길이 저장 실패:", error));
}

// 자동 저장 인터벌
setInterval(() => {
    if (player && player.getPlayerState && player.getPlayerState() === YT.PlayerState.PLAYING) {
        saveCurrentTime();
        saveProgressToServer(Math.floor(player.getCurrentTime()));
    }
}, 10000);

// 페이지 이탈 감지
function handlePageExit() {
    if (player && typeof player.getCurrentTime === 'function') {
        const currentTime = Math.floor(player.getCurrentTime());
        if (currentTime > 0) {
            saveCurrentTime(currentTime);
            saveProgressToServer(currentTime);
        }
    }
}
window.addEventListener('beforeunload', handlePageExit);
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') handlePageExit();
});

// 유튜브 API 수동 트리거
if (window.YT && window.YT.Player && typeof window.YT.Player === 'function') {
    window.onYouTubeIframeAPIReady();
}


/* =========================================
   3. UI 제어 (패널, 에디터, 챕터 전환)
   ========================================= */

let currentActiveTab = null;

// [패널 열기 함수]
function openPanel(tabName) {
    const wrapper = document.getElementById('side-panel-wrapper');
    const contentId = 'content-' + tabName;
    const targetContent = document.getElementById(contentId);

    // 이미 열려있고 같은 탭이면 닫기
    if (wrapper.classList.contains('open') && currentActiveTab === tabName) {
        closePanel();
        return;
    }

    // 모든 컨텐츠 숨기고 타겟만 표시
    document.querySelectorAll('.panel-content-box').forEach(el => el.style.display = 'none');
    if (targetContent) targetContent.style.display = 'flex';

    if (!wrapper.classList.contains('open')) wrapper.classList.add('open');

    // 탭별 특수 동작
    if (tabName === 'quiz') {
        if(currentChapterId) loadQuiz(currentChapterId);
    }
    if (tabName === 'interpreter' && monacoEditor){
        setTimeout(() => monacoEditor.layout(), 100);
    }

    currentActiveTab = tabName;
}

function closePanel() {
    document.getElementById('side-panel-wrapper').classList.remove('open');
    currentActiveTab = null;
}

function toggleSection(headerElement) {
    headerElement.classList.toggle('collapsed');
}

// [핵심] 챕터 클릭 시 실행되는 함수 (HTML에서 th:onclick으로 호출)
function playContent(chapterId, videoUrl) {
    // 1. 퀴즈 챕터인 경우
    if (videoUrl === 'QUIZ') {
        // (1) 유튜브 플레이어 숨기기
        const playerDiv = document.getElementById('player');
        if (playerDiv) playerDiv.style.display = 'none'; // 숨김
        if (player && typeof player.pauseVideo === 'function') {
            player.pauseVideo();
        }

        // (2) 퀴즈 영역 보여주기
        const quizWrapper = document.getElementById('quiz-wrapper');
        if (quizWrapper) quizWrapper.style.display = 'block'; // 표시

        // (3) 전역 변수 업데이트 및 로드
        currentChapterId = chapterId;
        loadQuiz(chapterId); // 데이터 불러오기

        return;
    }

    // 2. 일반 비디오인 경우
    // (1) 퀴즈 영역 숨기고 플레이어 보이기 (혹시 퀴즈 보고 왔을 수 있으니)
    const quizWrapper = document.getElementById('quiz-wrapper');
    if (quizWrapper) quizWrapper.style.display = 'none';

    const playerDiv = document.getElementById('player');
    if (playerDiv) playerDiv.style.display = 'block';

    // (2) 페이지 이동 (Spring Boot SSR)
    const courseId = document.getElementById('course-id').value;
    window.location.href = `/course/play?courseId=${courseId}&chapterId=${chapterId}`;
}

// [추가] 초기 로드 시 퀴즈 챕터인지 확인하는 로직 (페이지 로드될 때 실행)
document.addEventListener('DOMContentLoaded', () => {
    const initVideoUrl = document.getElementById('video-url').value;
    const initChapterId = document.getElementById('chapter-id').value;

    if (initVideoUrl === 'QUIZ') {
        playContent(initChapterId, 'QUIZ');
    }
});

const langSelector = document.getElementById('language-selector');
if(langSelector) {
    langSelector.addEventListener('change', function() {
        const langId = this.value;
        let langMode = 'python';
        let sampleCode = "print('Hello, LearnIT!')";

        if(langId === '62') { langMode = 'java'; sampleCode = 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, Java!");\n    }\n}'; }
        else if(langId === '63') { langMode = 'javascript'; sampleCode = "console.log('Hello, JS!');"; }
        else if(langId === '54') { langMode = 'cpp'; sampleCode = '#include <iostream>\n\nint main() {\n    std::cout << "Hello, C++!";\n    return 0;\n}'; }

        monaco.editor.setModelLanguage(monacoEditor.getModel(), langMode);
        monacoEditor.setValue(sampleCode);
    });
}

function getCsrfHeader() {
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    return (headerMeta && tokenMeta) ? { [headerMeta.content]: tokenMeta.content } : {};
}

function runCode() {
    const code = monacoEditor.getValue();
    const languageId = document.getElementById('language-selector').value;
    const consoleDiv = document.getElementById('output-console');

    consoleDiv.innerText = "실행 중입니다...";

    fetch('/api/interpreter/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getCsrfHeader() },
        body: JSON.stringify({ code: code, languageId: languageId })
    })
        .then(res => res.json())
        .then(data => { consoleDiv.innerText = data.output; })
        .catch(err => { console.error(err); consoleDiv.innerText = "에러 발생: " + err; });
}


/* =========================================
   5. [퀴즈 시스템] 통합 로직 (수정됨)
   ========================================= */

function loadQuiz(chapterId) {
    console.log("퀴즈 로드 요청: " + chapterId);

    fetch(`/api/quiz?chapterId=${chapterId}`)
        .then(response => {
            // [중요] 403 Forbidden: 수강 미달 시
            if (response.status === 403) {
                return response.text().then(msg => { throw new Error(msg); });
            }
            if (response.status === 204) {
                alert("이 챕터에는 등록된 퀴즈가 없습니다.");
                closePanel();
                return null;
            }
            return response.json();
        })
        .then(data => {
            if (!data) return;

            quizData = data;

            // HTML 업데이트 (제목 등)
            const titleEl = document.getElementById('display-quiz-title');
            if(titleEl) titleEl.innerText = data.title;

            // 상태 초기화
            currentQIndex = 0;
            userAnswers = [];
            showStep('start');
        })
        .catch(error => {
            console.warn("퀴즈 접근 불가:", error.message);
            alert("⚠️ " + error.message);
            closePanel();
        });
}

function startQuizLogic() {
    if (!quizData || !quizData.questions || quizData.questions.length === 0) {
        alert("퀴즈 데이터가 없습니다.");
        return;
    }
    showStep('question');
    renderQuestion();
}

function renderQuestion() {
    const question = quizData.questions[currentQIndex];
    const totalCount = quizData.questions.length;

    // UI 업데이트
    document.getElementById('curr-q-idx').innerText = currentQIndex + 1;
    document.getElementById('question-content').innerText = question.content;

    // 보기 생성
    const container = document.getElementById('options-container');
    container.innerHTML = '';

    question.options.forEach(opt => {
        const btn = document.createElement('div');
        btn.className = 'option-item';
        btn.innerText = opt.content;
        btn.onclick = () => selectOption(btn, question.questionId, opt.optionId);
        container.appendChild(btn);
    });

    // 버튼 초기화
    const nextBtn = document.getElementById('btn-next-question');
    nextBtn.disabled = true;
    nextBtn.style.backgroundColor = "#ccc";
    nextBtn.innerText = (currentQIndex === totalCount - 1) ? '제출 하기' : '다음 문제';
}

function selectOption(btnElement, qId, oId) {
    document.querySelectorAll('.option-item').forEach(el => el.classList.remove('selected'));
    btnElement.classList.add('selected');

    // 답안 저장/수정
    const existing = userAnswers.find(a => a.questionId === qId);
    if (existing) existing.optionId = oId;
    else userAnswers.push({ questionId: qId, optionId: oId });

    // 버튼 활성화
    const nextBtn = document.getElementById('btn-next-question');
    nextBtn.disabled = false;
    nextBtn.style.backgroundColor = (currentQIndex === quizData.questions.length - 1) ? "#00c471" : "#333";
}

function nextQuestion() {
    if (currentQIndex === quizData.questions.length - 1) {
        submitQuiz();
    } else {
        currentQIndex++;
        renderQuestion();
    }
}

function submitQuiz() {
    if (userAnswers.length < quizData.questions.length) {
        alert("모든 문제를 풀어주세요.");
        return;
    }

    const payload = {
        quizId: quizData.quizId,
        answers: userAnswers
    };

    fetch('/api/quiz/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getCsrfHeader() },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(result => {
            renderResult(result);
            showStep('result');
        })
        .catch(err => {
            console.error("제출 오류:", err);
            alert("채점 중 오류가 발생했습니다.");
        });
}

function renderResult(result) {
    const scoreEl = document.getElementById('result-score');
    if(scoreEl) scoreEl.innerText = result.score;

    const msgEl = document.getElementById('result-msg');
    if(msgEl) {
        msgEl.innerText = result.isPassed ? "축하합니다! 합격입니다 🎉" : "아쉽네요. 다시 도전해보세요 💪";
        msgEl.style.color = result.isPassed ? "#00c471" : "#ff4d4f";
    }

    // 다음 강의 버튼 활성화 여부 등 처리 가능
}

function showStep(stepName) {
    document.querySelectorAll('.quiz-step').forEach(el => el.style.display = 'none');
    const target = document.getElementById(`quiz-step-${stepName}`);
    if(target) target.style.display = (stepName === 'question') ? 'block' : 'flex';
}