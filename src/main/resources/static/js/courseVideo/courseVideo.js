
/* =========================================
   1. 전역 상태 및 데이터 관리
   ========================================= */
const state = {
    // DOM Elements Values
    videoUrl: document.getElementById('video-url')?.value || null,
    courseId: document.getElementById('course-id')?.value || null,
    chapterId: document.getElementById('chapter-id')?.value || null,
    nextChapterId: document.getElementById('next-chapter-id')?.value || null,

    // Instances
    player: null,       // YouTube Player Instance
    monacoEditor: null, // Monaco Editor Instance

    // Quiz State
    quizData: null,
    userAnswers: [],
    currentQIndex: 0,
    currentCorrectCount: 0,
    isGraded: false,

    // UI State
    currentActiveTab: null
};

// 초기화
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    if (state.videoUrl === 'QUIZ') {
        playContent(state.chapterId, 'QUIZ');
    }

    // Restore side panel state
    const urlParams = new URLSearchParams(window.location.search);
    const openTab = urlParams.get('openTab');

    if (openTab) {
        openPanel(openTab);
    } else {
        const savedTab = localStorage.getItem('lastActivePanel');
        if (savedTab) {
            openPanel(savedTab);
        }
    }
}

function goBackOrHome() {
    const courseId = document.getElementById('course-id')?.value;
    
    // Check if referrer exists, matches our host, AND is NOT from another course video page
    if (document.referrer && 
        document.referrer.indexOf(window.location.host) !== -1 &&
        document.referrer.indexOf('/course/play') === -1) {
        history.back();
    } else {
        if (courseId) {
            location.href = `/CourseDetail?courseId=${courseId}&tab=intro`;
        } else {
            location.href = '/'; // Fallback to home if courseId is missing
        }
    }
}

/* =========================================
   2. Helper Functions (API, Utils)
   ========================================= */

function getCsrfHeader() {
    const header = document.querySelector('meta[name="_csrf_header"]');
    const token = document.querySelector('meta[name="_csrf"]');
    return (header && token) ? { [header.content]: token.content } : {};
}

// 공통 Fetch 함수 (CSRF 헤더 포함) + 세션쿠키 포함(도커/프록시/크로스도메인 대비)
async function fetchWithCsrf(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...getCsrfHeader(),
        ...(options.headers || {})
    };

    const res = await fetch(url, {
        credentials: 'include', // ✅ JSESSIONID 등 세션쿠키 무조건 포함
        ...options,
        headers
    });

    // ✅ fetch가 /login 으로 리다이렉트된 경우를 프론트에서 감지 가능하게
    // (서버가 302 -> /login 처리하면 fetch는 최종 HTML을 받아서 ok가 되어버릴 수 있음)
    if (res.redirected && res.url && res.url.includes('/login')) {
        // 로그인 필요 상황을 명확히 처리하기 위해 401처럼 다루기
        // 호출부에서 res.status가 401은 아니지만, 여기서 throw로 처리 가능
        // (원하면 throw 대신 return res로 두고 호출부에서 res.url 체크해도 됨)
        throw new Error("LOGIN_REQUIRED");
    }

    return res;
}

/* =========================================
   3. YouTube Player & 진도율 관리
   ========================================= */

function getVideoId(url) {
    if (!url || url === 'QUIZ') return null;
    try {
        const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
        const match = url.match(regExp);
        return (match && match[2].length === 11) ? match[2] : new URL(url).searchParams.get("v");
    } catch (e) {
        return null;
    }
}

const currentVideoId = getVideoId(state.videoUrl);
const STORAGE_KEY = `yt-time-${currentVideoId || "default"}`;
let savedTime = Number(localStorage.getItem(STORAGE_KEY)) || 0;

window.onYouTubeIframeAPIReady = function() {
    if (!currentVideoId) return;
    state.player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        videoId: currentVideoId,
        playerVars: { 'start': 0, 'rel': 0, 'autoplay': 0 },
        events: {
            'onReady': onPlayerReady,
            'onStateChange': onPlayerStateChange
        }
    });
};

function onPlayerReady(event) {
    if (state.player?.getDuration) {
        const duration = Math.floor(state.player.getDuration());
        if (duration > 0) saveDurationToServer(duration);
    }

    if (savedTime > 0) {
        const min = Math.floor(savedTime / 60);
        const sec = Math.floor(savedTime % 60);
        if (confirm(`이전 시청 기록(${min}분 ${sec}초)이 있습니다. 이어 보시겠습니까?`)) {
            state.player.seekTo(savedTime);
        } else {
            state.player.seekTo(0);
        }
    }
}

function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.PAUSED) saveProgress();
    if (event.data === YT.PlayerState.ENDED) localStorage.removeItem(STORAGE_KEY);
}

function saveProgress() {
    if (!state.player?.getCurrentTime) return;
    const time = Math.floor(state.player.getCurrentTime());
    localStorage.setItem(STORAGE_KEY, time);

    if (state.videoUrl !== 'QUIZ' && state.courseId && state.chapterId) {
        fetchWithCsrf(`/course/log?courseId=${state.courseId}&chapterId=${state.chapterId}`, {
            method: 'POST',
            body: JSON.stringify({ playTime: time }),
            keepalive: true
        }).catch(console.error);
    }
}

setInterval(() => {
    if (state.player?.getPlayerState?.() === YT.PlayerState.PLAYING) saveProgress();
}, 10000);

window.addEventListener('beforeunload', saveProgress);

function saveDurationToServer(duration) {
    if (!state.chapterId || state.videoUrl === 'QUIZ') return;
    fetchWithCsrf(`/course/log/duration?chapterId=${state.chapterId}&duration=${duration}`, { method: 'POST' })
        .catch(console.error);
}

/* =========================================
   4. UI 제어 (패널 및 화면 전환)
   ========================================= */

function openPanel(tabName) {
    const wrapper = document.getElementById('side-panel-wrapper');
    const targetId = (tabName === 'reference') ? 'content-reference' : 'content-' + tabName;
    const targetContent = document.getElementById(targetId);

    if (wrapper.classList.contains('open') && state.currentActiveTab === tabName) {
        closePanel();
        return;
    }

    document.querySelectorAll('.panel-content-box').forEach(el => el.style.display = 'none');
    if (targetContent) targetContent.style.display = 'block';

    if (!wrapper.classList.contains('open')) wrapper.classList.add('open');

    if (tabName === 'interpreter' && state.monacoEditor) {
        setTimeout(() => state.monacoEditor.layout(), 100);
    }

    if (tabName === 'reference') loadResources();

    if (tabName === 'qna') initQnaPanel();

    state.currentActiveTab = tabName;
    localStorage.setItem('lastActivePanel', tabName);
}

function closePanel() {
    document.getElementById('side-panel-wrapper').classList.remove('open');
    state.currentActiveTab = null;
    localStorage.removeItem('lastActivePanel');
}

function toggleSection(headerElement) {
    headerElement.classList.toggle('collapsed');
}

function playContent(chapterId, videoUrl) {
    const videoWrapper = document.querySelector('.video-wrapper');
    const quizWrapper = document.getElementById('quiz-wrapper');
    const playerDiv = document.getElementById('player');

    if (videoUrl === 'QUIZ') {
        videoWrapper?.classList.add('quiz-active');
        if (playerDiv) playerDiv.style.display = 'none';
        state.player?.pauseVideo?.();

        if (quizWrapper) quizWrapper.style.display = 'block';

        state.chapterId = chapterId;
        loadQuiz(chapterId);
        updateSidebarActive(chapterId, true);
    } else {
        videoWrapper?.classList.remove('quiz-active');
        if (quizWrapper) quizWrapper.style.display = 'none';
        if (playerDiv) playerDiv.style.display = 'block';

        window.location.href = `/course/play?courseId=${state.courseId}&chapterId=${chapterId}`;
    }
}

function updateSidebarActive(id, isQuiz) {
    document.querySelectorAll('.chapter-list li').forEach(li => li.classList.remove('active'));

    const links = document.querySelectorAll(`.chapter-list a[data-id="${id}"]`);
    let targetLink = null;

    links.forEach(link => {
        const url = link.getAttribute('data-url');
        if (isQuiz && url === 'QUIZ') {
            targetLink = link;
        } else if (!isQuiz && url !== 'QUIZ') {
            targetLink = link;
        }
    });

    if (targetLink && targetLink.parentElement) {
        targetLink.parentElement.classList.add('active');
    }
}

/* =========================================
   5. Monaco Editor & Interpreter
   ========================================= */

require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.34.1/min/vs' }});
require(['vs/editor/editor.main'], function () {
    const container = document.getElementById('monaco-editor-container');
    if (!container) return;

    state.monacoEditor = monaco.editor.create(container, {
        value: "print('Hello, LearnIT!')",
        language: 'python',
        theme: 'vs-light',
        lineNumbersMinChars: 3,
        automaticLayout: true
    });
});

document.getElementById('language-selector')?.addEventListener('change', function() {
    const presets = {
        '62': { mode: 'java', code: 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, Java!");\n    }\n}' },
        '63': { mode: 'javascript', code: "console.log('Hello, JS!');" }
    };
    const config = presets[this.value] || presets['71'];
    monaco.editor.setModelLanguage(state.monacoEditor.getModel(), config.mode);
    state.monacoEditor.setValue(config.code);
});

function runCode() {
    const consoleDiv = document.getElementById('output-console');
    consoleDiv.innerText = "실행 중입니다...";

    // courseId와 chapterId 가져오기
    const courseId = document.getElementById('course-id')?.value;
    const chapterId = document.getElementById('chapter-id')?.value;

    fetchWithCsrf('/api/interpreter/run', {
        method: 'POST',
        body: JSON.stringify({
            code: state.monacoEditor.getValue(),
            languageId: document.getElementById('language-selector').value,
            courseId: courseId,
            chapterId: chapterId
        })
    })
        .then(res => res.json())
        .then(data => { consoleDiv.innerText = data.output; })
        .catch(err => { consoleDiv.innerText = "에러 발생: " + err; });
}

/* =========================================
   6. Quiz System
   ========================================= */

function loadQuiz(chapterId) {
    fetch(`/api/quiz?chapterId=${chapterId}`)
        .then(res => {
            if (res.status === 403) return res.text().then(m => { throw new Error(m); });
            return res.json();
        })
        .then(data => {
            if (!data) return;
            state.quizData = data;
            document.getElementById('display-quiz-title').innerText = data.title;
            state.currentQIndex = 0;
            state.currentCorrectCount = 0;
            state.userAnswers = [];
            
            if (data.submitted) {
                 showQuizResultUI();
            }
            
            showStep('start');
        })
        .catch(err => alert("오류: " + err.message));
}

function startQuizLogic() {
    if (!state.quizData?.questions?.length) return alert("퀴즈 데이터를 불러올 수 없습니다.");
    showStep('question');
    renderQuizQuestion();
}

function renderQuizQuestion() {
    state.isGraded = false;
    const question = state.quizData.questions[state.currentQIndex];

    document.getElementById('total-q-count').innerText = state.quizData.questions.length;
    document.getElementById('current-q-num').innerText = state.currentQIndex + 1;
    document.getElementById('question-content').innerText = question.questionContent;

    const expText = question.explanation ? question.explanation : "별도의 해설이 없습니다.";
    document.getElementById('explanation-text').innerText = expText;
    document.getElementById('explanation-area').style.display = 'none';

    const container = document.getElementById('options-container');
    container.innerHTML = '';
    container.classList.remove('graded');

    question.options.forEach(opt => {
        const btn = document.createElement('div');
        btn.className = 'option-item';
        btn.innerText = opt.optionContent;
        btn.onclick = () => {
            if (!state.isGraded) selectOption(btn, question.questionId, opt.optionId);
        };
        container.appendChild(btn);
    });

    const nextBtn = document.getElementById('btn-next-question');
    nextBtn.disabled = true;
    nextBtn.style.backgroundColor = "#ccc";
    nextBtn.innerText = '제출하기';
}

function selectOption(btn, qId, oId) {
    document.querySelectorAll('.option-item').forEach(el => el.classList.remove('selected'));
    btn.classList.add('selected');

    const ansIdx = state.userAnswers.findIndex(a => a.questionId === qId);
    if (ansIdx > -1) state.userAnswers[ansIdx].optionId = oId;
    else state.userAnswers.push({ questionId: qId, optionId: oId });

    const nextBtn = document.getElementById('btn-next-question');
    nextBtn.disabled = false;
    nextBtn.style.backgroundColor = "#333";
}

function handleQuizAction() {
    if (!state.isGraded) checkAnswerLocally();
    else nextQuestion();
}

function checkAnswerLocally() {
    const question = state.quizData.questions[state.currentQIndex];
    state.isGraded = true;
    
    document.getElementById('options-container').classList.add('graded');
    
    // 정답 체크 로직: "T", "Y", true, 1 모두 허용
    const isTrue = (val) => ['T', 'Y', 'TRUE', '1'].includes(String(val).toUpperCase().trim());
    
    const selectedAnswer = state.userAnswers.find(a => a.questionId === question.questionId);
    const selectedOptionId = selectedAnswer ? String(selectedAnswer.optionId) : null;

    document.querySelectorAll('.option-item').forEach((btn, idx) => {
        const opt = question.options[idx];
        const isCorrect = isTrue(opt.isCorrect || opt.is_correct || opt.correct);

        if (isCorrect) btn.classList.add('correct');
        
        if (String(opt.optionId) === selectedOptionId) {
            if (!isCorrect) btn.classList.add('wrong');
            else state.currentCorrectCount++;
        }
    });

    document.getElementById('explanation-area').style.display = 'block';

    const nextBtn = document.getElementById('btn-next-question');
    const isLast = state.currentQIndex === state.quizData.questions.length - 1;
    nextBtn.innerText = isLast ? '결과 보기' : '다음 문제';
    nextBtn.style.backgroundColor = isLast ? "#00c471" : "#333";
}

function nextQuestion() {
    if (state.currentQIndex === state.quizData.questions.length - 1) submitQuizFinal();
    else {
        state.currentQIndex++;
        renderQuizQuestion();
    }
}

function showQuizResultUI() {
    const btn = document.querySelector('#quiz-step-result button');
    let finalQuizId = document.getElementById('final-quiz-id')?.value;
    
    if (finalQuizId === 'null' || finalQuizId === '') finalQuizId = null;

    const currentQuizId = state.quizData.quizId;
    const isFinalSubmitted = document.getElementById('is-final-submitted')?.value === 'true';

    // 1. 현재 푼 퀴즈가 파이널 퀴즈인 경우
    if (finalQuizId && String(currentQuizId) === String(finalQuizId)) {
        btn.innerText = "수강 완료 (메인으로)";
        btn.style.backgroundColor = "#333";
        btn.onclick = () => { location.href = '/' };
        document.getElementById('is-final-submitted').value = 'true';
    }
    // 2. 마지막 섹션 퀴즈 (다음 강의 없음)
    else if ((!state.nextChapterId || state.nextChapterId === 'null') && finalQuizId) {
        if (isFinalSubmitted) {
            btn.innerText = "파이널 퀴즈 다시 풀기";
            btn.style.backgroundColor = "#ff6b6b";
            btn.onclick = () => { 
                if(confirm("이미 파이널 퀴즈를 완료하셨습니다. 다시 푸시겠습니까?")) {
                    playContent(finalQuizId, 'QUIZ'); 
                }
            };
        } else {
            btn.innerText = "파이널 퀴즈 도전";
            btn.style.backgroundColor = "#ff6b6b";
            btn.onclick = () => { playContent(finalQuizId, 'QUIZ'); };
        }
    }
    // 3. 일반 퀴즈
    else {
        btn.innerText = "다음 강의 보기";
        btn.style.backgroundColor = "#333";
        btn.onclick = () => { goToNextChapter(); };
    }
}

function submitQuizFinal() {
    fetchWithCsrf('/api/quiz/submit', {
        method: 'POST',
        body: JSON.stringify({ quizId: state.quizData.quizId, answers: state.userAnswers })
    })
        .then(res => res.json())
        .then(result => {
            const ratioScore = Math.round((state.currentCorrectCount / state.quizData.questions.length) * 100);
            renderFinalResult(ratioScore, result.isPassed);
            showQuizResultUI();
            showStep('result');
        })
        .catch(() => alert("결과 전송 중 오류가 발생했습니다."));
}

function renderFinalResult(score, isPassed) {
    document.getElementById('result-score').innerText = score;
}

function showStep(stepName) {
    document.querySelectorAll('.quiz-step').forEach(el => el.style.display = 'none');
    const target = document.getElementById(`quiz-step-${stepName}`);
    if (target) target.style.display = (stepName === 'question') ? 'block' : 'flex';
}

function goToNextChapter() {
    const nextChapterId = state.nextChapterId;
    const finalQuizIdElement = document.getElementById('final-quiz-id');
    const finalQuizId = finalQuizIdElement ? finalQuizIdElement.value : null;
    const currentQuizId = state.quizData.quizId;

    if (nextChapterId) {
        location.href = `/course/play?courseId=${state.courseId}&chapterId=${nextChapterId}`;
        return;
    }

    if (finalQuizId && String(currentQuizId) !== String(finalQuizId)) {
        playContent(finalQuizId, 'QUIZ');
        return;
    }

    alert("모든 강의와 평가를 완료했습니다. 수고하셨습니다.");
    location.href = `/course/detail?courseId=${state.courseId}`;
}

/* =========================================
   7. Resource (자료실)
   ========================================= */

function loadResources() {
    const listContainer = document.getElementById('resource-list');
    const emptyMsg = document.getElementById('no-resource-msg');

    listContainer.innerHTML = '';
    emptyMsg.style.display = 'none';

    fetch(`/api/resources?courseId=${state.courseId}`)
        .then(res => res.ok ? res.json() : Promise.reject("자료실 로딩 실패"))
        .then(data => {
            if (!data || data.length === 0) {
                emptyMsg.style.display = 'block';
                return;
            }
            data.forEach(item => {
                const li = document.createElement('li');
                li.className = 'resource-item';
                const typeStr = (item.fileType || 'FILE').toUpperCase();
                let badgeClass = 'badge-default';
                if (typeStr === 'PDF') badgeClass = 'badge-pdf';
                else if (typeStr === 'ZIP') badgeClass = 'badge-zip';

                li.innerHTML = `
                <div class="res-info">
                    <div class="res-title">
                        <span class="badge-type ${badgeClass}">${typeStr}</span>
                        <span class="text-content">${item.title}</span>
                    </div>
                </div>
                <a href="${item.fileUrl}" class="btn-download" download target="_blank" title="다운로드">
                    <img src="/images/course/icon-file-download.png" alt="다운로드">
                </a>
            `;
                listContainer.appendChild(li);
            });
        })
        .catch(err => {
            console.error(err);
            listContainer.innerHTML = '<li style="padding:15px; text-align:center;">자료를 불러오지 못했습니다.</li>';
        });
}
