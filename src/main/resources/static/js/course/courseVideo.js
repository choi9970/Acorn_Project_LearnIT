// HTML hidden input에서 정보 가져오기 (URL, CourseID, ChapterID)
const videoInput = document.getElementById('video-url');
const courseInput = document.getElementById('course-id');
const chapterInput = document.getElementById('chapter-id');

const dbVideoUrl = videoInput ? videoInput.value : null;
const currentCourseId = courseInput ? courseInput.value : null;
const currentChapterId = chapterInput ? chapterInput.value : null;

let monacoEditor = null;

// ID 추출 함수 (모든 유튜브 주소 형식 대응)
function getVideoId(url) {
    if (!url) return null;
    try {
        const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
        const match = url.match(regExp);
        if (match && match[2].length === 11) return match[2];
        const urlObj = new URL(url);
        return urlObj.searchParams.get("v");
    } catch (e) {
        console.error("URL 파싱 실패:", e);
        return null;
    }
}

const currentVideoId = getVideoId(dbVideoUrl);
const storageKey = "yt-time-" + currentVideoId;
let player;

// 저장된 시간 불러오기
let savedTime = 0;
try {
    const time = localStorage.getItem(storageKey);
    savedTime = (time && !isNaN(time)) ? Number(time) : 0;
} catch (e) {
    savedTime = 0;
}

// 유튜브 API 준비되면 실행 (window 전역 객체에 등록)
window.onYouTubeIframeAPIReady = function() {
    if (!currentVideoId) return;

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

// 상태 변화 감지
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

// 로컬 스토리지에 시간 저장 (이어보기용)
function saveCurrentTime(time) {    //추가
    if(time === undefined && player) time = Math.floor(player.getCurrentTime());
    localStorage.setItem(storageKey, time);
}

// 서버로 진도율 전송 (DB 저장용)
function saveProgressToServer(time) {
    if (!currentCourseId || !currentChapterId) return;

    const payload = { playTime: time };
    const url = `/course/log?courseId=${currentCourseId}&chapterId=${currentChapterId}`;

    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        keepalive: true
    })
        .then(response => {
            if (!response.ok) console.error("서버 저장 실패");
        })
        .catch(error => console.error("통신 에러:", error));
}

// 자동 저장 (10초마다) - 로컬스토리지 & 서버 둘 다 저장
setInterval(() => {
    if (player && player.getPlayerState && player.getPlayerState() === YT.PlayerState.PLAYING) {
        const currentTime = Math.floor(player.getCurrentTime());

        // 1. 이어보기 저장
        saveCurrentTime();

        // 2. 서버 DB로 진도율 전송
        saveProgressToServer(currentTime);
    }
}, 10000);

function handlePageExit() {
    if (player && typeof player.getCurrentTime === 'function') {
        const currentTime = Math.floor(player.getCurrentTime());

        // 0초 이상일 때만 저장
        if (currentTime > 0) {
            saveCurrentTime(currentTime);      // 로컬 스토리지 저장
            saveProgressToServer(currentTime); // 서버 DB 저장
            console.log("페이지 이탈 감지 저장:", currentTime);
        }
    }
}

// 브라우저 닫기, 새로고침, 탭 닫기 감지
window.addEventListener('beforeunload', handlePageExit);

// 모바일: 탭 전환, 최소화, 홈 화면 이동
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
        handlePageExit();
    }
});

// API가 먼저 로드되었을 경우를 대비해 수동 실행
if (window.YT && window.YT.Player && typeof window.YT.Player === 'function') {
    window.onYouTubeIframeAPIReady();
}

// 현재 열린 탭 ID 기억
let currentActiveTab = null;

// 패널 열기 & 탭 전환 함수
function openPanel(tabName) {
    const wrapper = document.getElementById('side-panel-wrapper');
    const contentId = 'content-' + tabName;
    const targetContent = document.getElementById(contentId);

    if (wrapper.classList.contains('open') && currentActiveTab === tabName) {
        closePanel();
        return;
    }

    const allContents = document.querySelectorAll('.panel-content-box');
    allContents.forEach(el => el.style.display = 'none');

    if (targetContent) {
        targetContent.style.display = 'flex'; // flex로 보여야 내부 레이아웃 유지됨
    }

    if (!wrapper.classList.contains('open')) {
        wrapper.classList.add('open');
    }

    if (tabName === 'quiz') {
        if(currentChapterId) loadQuiz(currentChapterId);
    }

    currentActiveTab = tabName;

    if (tabName === 'interpreter' && monacoEditor){
        setTimeout(() => {
            monacoEditor.layout();
        }, 100);
    }
}

// 패널 닫기 함수 (X 버튼용)
function closePanel() {
    const wrapper = document.getElementById('side-panel-wrapper');
    wrapper.classList.remove('open');
    currentActiveTab = null; // 상태 초기화
}

// 플레이어가 로딩되자마자 실행되는 함수
function onPlayerReady(event) {
    if(player && player.getDuration) {
        const duration = Math.floor(player.getDuration());
        if (duration > 0) {
            saveDurationToServer(duration);
        }
    }

    if(savedTime > 0) {
        player.seekTo(savedTime);
    }
}

// 서버로 전체 시간(duration) 전송 함수
function saveDurationToServer(duration) {
    if (!currentChapterId) return;

    const url = `/course/log/duration?chapterId=${currentChapterId}&duration=${duration}`;

    fetch(url, {
        method: 'POST',
    })
        .then(response => {
            if (response.ok) console.log("DB에 영상 길이 저장 완료");
        })
        .catch(error => console.error("영상 길이 저장 실패:", error));
}

function toggleSection(headerElement) {
    headerElement.classList.toggle('collapsed');
}

// Monaco Editor 로드
require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.34.1/min/vs' }});

require(['vs/editor/editor.main'], function () {
    monacoEditor = monaco.editor.create(document.getElementById('monaco-editor-container'), {
        value: "print('Hello, LearnIT!')",
        language: 'python',
        theme: 'vs-light',
        lineNumbersMinChars: 3,
        glyphMargin: false,
        folding: false,
        lineDecorationsWidth: 0,
        overviewRulerBorder: false,
        minimap: { enabled: false },
        automaticLayout: true,
        scrollBeyondLastLine: false,
        scrollbar: {
            vertical: 'auto',
            horizontal: 'auto',
            verticalScrollbarSize: 10,
            horizontalScrollbarSize: 10
        }
    });
});

// 언어 변경 시 에디터 언어 설정 변경
document.getElementById('language-selector').addEventListener('change', function() {
    const langId = this.value;
    let langMode = 'python';
    let sampleCode = "print('Hello, LearnIT!')";

    if(langId === '62') { langMode = 'java'; sampleCode = 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, Java!");\n    }\n}'; }
    else if(langId === '63') { langMode = 'javascript'; sampleCode = "console.log('Hello, JS!');"; }
    else if(langId === '54') { langMode = 'cpp'; sampleCode = '#include <iostream>\n\nint main() {\n    std::cout << "Hello, C++!";\n    return 0;\n}'; }

    monaco.editor.setModelLanguage(monacoEditor.getModel(), langMode);
    monacoEditor.setValue(sampleCode);
});

function getCsrfHeader() {
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    const tokenMeta = document.querySelector('meta[name="_csrf"]');

    if (!headerMeta || !tokenMeta) {
        return {};
    }

    return { [headerMeta.content]: tokenMeta.content };
}

// 코드 실행 함수 (Ajax -> Spring Boot -> Judge0)
function runCode() {
    const code = monacoEditor.getValue();
    const languageId = document.getElementById('language-selector').value;
    const consoleDiv = document.getElementById('output-console');

    consoleDiv.innerText = "실행 중입니다...";

    // [중요] CSRF 토큰 (기존에 만든 getCsrfHeader 함수 사용)
    fetch('/api/interpreter/run', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...getCsrfHeader() // CSRF 토큰 포함
        },
        body: JSON.stringify({
            code: code,
            languageId: languageId
        })
    })
        .then(res => res.json())
        .then(data => {
            consoleDiv.innerText = data.output;
        })
        .catch(err => {
            console.error(err);
            consoleDiv.innerText = "에러 발생: " + err;
        });
}

// 상태 변수들
let quizData = null;      // 문제 데이터 (서버에서 받아옴)
let userAnswers = [];     // 사용자가 선택한 답 [{questionId: 1, optionId: 3}]
let currentQIndex = 0;    // 현재 몇 번 문제인지 (0부터 시작)

// 1. 퀴즈 데이터 로드 (패널 열릴 때 호출)
function loadQuiz(chapterId) {
    // 로딩 중 표시 등 필요하면 추가
    fetch(`/api/quiz?chapterId=${chapterId}`)
        .then(res => res.json())
        .then(data => {
            if (!data || data.questions.length === 0) {
                alert("이 강의에는 아직 퀴즈가 없습니다.");
                closePanel();
                return;
            }
            quizData = data;

            // 시작 화면 세팅
            document.getElementById('display-quiz-title').innerText = data.title;
            document.getElementById('display-total-count').innerText = data.questions.length;

            // 화면 초기화
            showStep('start');
        })
        .catch(err => {
            console.error("퀴즈 로드 실패:", err);
            alert("퀴즈 정보를 불러오지 못했습니다.");
        });
}

// 2. [시작하기] 버튼 클릭
function startQuizLogic() {
    currentQIndex = 0;
    userAnswers = [];
    showStep('question');
    renderQuestion();
}

// 3. 문제 렌더링 (현재 인덱스에 맞춰서)
function renderQuestion() {
    const question = quizData.questions[currentQIndex];
    const total = quizData.questions.length;

    // 진행 상태 업데이트
    document.getElementById('curr-q-idx').innerText = currentQIndex + 1;
    document.getElementById('total-q-idx').innerText = total;
    document.getElementById('quiz-progress-fill').style.width = ((currentQIndex + 1) / total * 100) + '%';

    // 질문 텍스트
    document.getElementById('question-content').innerText = question.content;

    // 보기 버튼 생성
    const container = document.getElementById('options-container');
    container.innerHTML = ''; // 기존 보기 비우기

    question.options.forEach(opt => {
        const btn = document.createElement('button');
        btn.className = 'option-item';
        btn.innerText = opt.content;
        btn.onclick = () => selectOption(btn, question.questionId, opt.optionId);
        container.appendChild(btn);
    });

    // 다음 버튼 초기화
    const nextBtn = document.getElementById('btn-next-question');
    nextBtn.disabled = true;
    nextBtn.innerText = (currentQIndex === total - 1) ? '제출하기' : '다음 문제';
}

// 4. 보기 선택 시
function selectOption(btnElement, qId, oId) {
    // 모든 버튼 선택 해제 스타일
    document.querySelectorAll('.option-item').forEach(el => el.classList.remove('selected'));

    // 클릭한 버튼 선택 스타일
    btnElement.classList.add('selected');

    // 답안 기록 (이미 있으면 덮어쓰기)
    const existing = userAnswers.find(a => a.questionId === qId);
    if (existing) {
        existing.optionId = oId;
    } else {
        userAnswers.push({ questionId: qId, optionId: oId });
    }

    // 다음 버튼 활성화
    document.getElementById('btn-next-question').disabled = false;
}

// 5. [다음 문제] / [제출] 버튼 클릭
function nextQuestion() {
    // 마지막 문제라면 제출
    if (currentQIndex === quizData.questions.length - 1) {
        submitQuiz();
    } else {
        currentQIndex++;
        renderQuestion();
    }
}

// 6. 퀴즈 제출 (서버로 채점 요청)
function submitQuiz() {
    const payload = {
        quizId: quizData.quizId,
        answers: userAnswers
    };

    fetch('/api/quiz/submit', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...getCsrfHeader() // CSRF 토큰 필수!
        },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(result => {
            renderResult(result);
            showStep('result');
        })
        .catch(err => {
            console.error("제출 실패:", err);
            alert("채점 중 오류가 발생했습니다.");
        });
}

// 7. 결과 화면 렌더링
function renderResult(result) {
    document.getElementById('result-score').innerText = result.score;

    const badge = document.getElementById('result-badge');
    const msg = document.getElementById('result-msg');

    if (result.isPassed) {
        badge.innerText = '합격';
        badge.className = 'result-badge pass';
        msg.innerText = "축하합니다! 이 섹션을 완벽하게 이해하셨군요.";
        msg.style.color = "#00c471";
    } else {
        badge.innerText = '불합격';
        badge.className = 'result-badge fail';
        msg.innerText = "조금 더 학습이 필요합니다. 다시 도전해보세요!";
        msg.style.color = "#ff4d4f";
    }

    // 오답 리스트 (리뷰)
    const reviewBox = document.getElementById('review-list');
    reviewBox.innerHTML = '';

    result.reviewList.forEach((item, idx) => {
        const div = document.createElement('div');
        div.className = item.correct ? 'review-item correct' : 'review-item wrong';
        div.innerHTML = `
            <span class="review-q">Q${idx + 1}. ${item.questionContent}</span>
            <span class="review-ans">
                ${item.correct ? '✅ 정답' : `❌ 오답 (정답: ${item.correctAnswer})`}
            </span>
            ${!item.correct ? `<div style="margin-top:4px; color:#888;">💡 해설: ${item.explanation}</div>` : ''}
        `;
        reviewBox.appendChild(div);
    });
}

// 유틸: 단계별 화면 전환
function showStep(stepName) {
    // 모든 단계 숨김
    document.querySelectorAll('.quiz-step').forEach(el => el.style.display = 'none');
    // 해당 단계만 표시
    document.getElementById(`quiz-step-${stepName}`).style.display = 'block';
}