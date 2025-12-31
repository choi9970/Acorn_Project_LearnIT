/* =========================================
   1. 전역 상태 및 데이터 관리
   ========================================= */
const state = {
    // DOM Inputs
    videoUrl: document.getElementById('video-url')?.value || null,
    courseId: document.getElementById('course-id')?.value || null,
    chapterId: document.getElementById('chapter-id')?.value || null,
    nextChapterId: document.getElementById('next-chapter-id')?.value || null,

    // Player & Editor Instances
    player: null,
    monacoEditor: null,

    // Quiz State
    quizData: null,
    userAnswers: [],
    currentQIndex: 0,
    currentCorrectCount: 0,
    isGraded: false, // 현재 문제 채점(제출) 완료 여부

    // UI State
    currentActiveTab: null
};

// 페이지 로드 시 초기화 실행
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

/** @description 어플리케이션 초기화 진입점 */
function initApp() {
    if (state.videoUrl === 'QUIZ') {
        playContent(state.chapterId, 'QUIZ');
    }
}

/* =========================================
   2. 유튜브 IFrame API 및 진도율 제어
   ========================================= */

/** @description URL에서 유튜브 Video ID 추출 */
function getVideoId(url) {
    if (!url || url === 'QUIZ') return null;
    try {
        const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
        const match = url.match(regExp);
        return (match && match[2].length === 11) ? match[2] : new URL(url).searchParams.get("v");
    } catch (e) { return null; }
}

const currentVideoId = getVideoId(state.videoUrl);
const STORAGE_KEY = `yt-time-${currentVideoId || "default"}`;
let savedTime = Number(localStorage.getItem(STORAGE_KEY)) || 0;

/** @description 유튜브 플레이어 API 콜백 */
window.onYouTubeIframeAPIReady = function() {
    if (!currentVideoId) return;
    state.player = new YT.Player('player', {
        height: '100%', width: '100%', videoId: currentVideoId,
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

        const userSelectResume = confirm(`\"${min}분 ${sec}초\"까지 영상을 시청하셨습니다.\n이어보시겠습니까?`);

        if(userSelectResume){
            state.player.seekTo(savedTime);
        }else{
            state.player.seekTo(0);
        }
    };
}

function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.PAUSED) saveProgress();
    if (event.data === YT.PlayerState.ENDED) localStorage.removeItem(STORAGE_KEY);
}

/** @description 실시간 시청 기록 및 서버 로그 저장 */
function saveProgress() {
    if (!state.player?.getCurrentTime) return;
    const time = Math.floor(state.player.getCurrentTime());
    localStorage.setItem(STORAGE_KEY, time);

    if (state.videoUrl !== 'QUIZ' && state.courseId && state.chapterId) {
        fetch(`/course/log?courseId=${state.courseId}&chapterId=${state.chapterId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playTime: time }),
            keepalive: true
        }).catch(console.error);
    }
}

// 10초 주기 자동 저장 및 이탈 시 저장
setInterval(() => {
    if (state.player?.getPlayerState?.() === YT.PlayerState.PLAYING) saveProgress();
}, 10000);
window.addEventListener('beforeunload', saveProgress);

function saveDurationToServer(duration) {
    if (!state.chapterId || state.videoUrl === 'QUIZ') return;
    fetch(`/course/log/duration?chapterId=${state.chapterId}&duration=${duration}`, { method: 'POST' })
        .catch(console.error);
}

/* =========================================
   3. UI 제어 (패널 및 화면 전환)
   ========================================= */

/** @description 사이드 패널 열기/닫기 및 에디터 레이아웃 갱신 */
function openPanel(tabName) {
    const wrapper = document.getElementById('side-panel-wrapper');

    // 탭 이름에 맞춰 타겟 ID 결정 ('reference' -> 'content-reference')
    // HTML ID가 'content-reference'이므로, 여기서 매핑을 맞춰줍니다.
    const targetId = (tabName === 'reference') ? 'content-reference' : 'content-' + tabName;
    const targetContent = document.getElementById(targetId);

    // 이미 열려있는 탭을 누르면 닫기
    if (wrapper.classList.contains('open') && state.currentActiveTab === tabName) {
        closePanel();
        return;
    }

    // 다른 패널들은 숨기고 타겟 패널만 보이기
    document.querySelectorAll('.panel-content-box').forEach(el => el.style.display = 'none');

    if (targetContent) {
        targetContent.style.display = 'block'; // flex 대신 block 권장 (내부 디자인에 따라 다름)
    }

    // 사이드바 열기
    if (!wrapper.classList.contains('open')) {
        wrapper.classList.add('open');
    }

    // 모나코 에디터 레이아웃 갱신 (인터프리터 탭일 경우)
    if (tabName === 'interpreter' && state.monacoEditor) {
        setTimeout(() => state.monacoEditor.layout(), 100);
    }

    // 자료실 탭을 열 때만 데이터 로딩 함수 실행
    if (tabName === 'reference') {
        loadResources();
    }

    state.currentActiveTab = tabName;
}

function closePanel() {
    document.getElementById('side-panel-wrapper').classList.remove('open');
    state.currentActiveTab = null;
}

function toggleSection(headerElement) {
    headerElement.classList.toggle('collapsed');
}

/** @description 영상 모드와 퀴즈 모드 간 동적 전환 */
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
    } else {
        videoWrapper?.classList.remove('quiz-active');
        if (quizWrapper) quizWrapper.style.display = 'none';
        if (playerDiv) playerDiv.style.display = 'block';

        window.location.href = `/course/play?courseId=${state.courseId}&chapterId=${chapterId}`;
    }
}

/* =========================================
   4. Monaco Editor & Interpreter
   ========================================= */

require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.34.1/min/vs' }});
require(['vs/editor/editor.main'], function () {
    const container = document.getElementById('monaco-editor-container');
    if(!container) return;

    state.monacoEditor = monaco.editor.create(container, {
        value: "print('Hello, LearnIT!')",
        language: 'python',
        theme: 'vs-light',
        lineNumbersMinChars: 3,
        automaticLayout: true
    });
});

/** @description 언어 변경 이벤트 핸들러 */
document.getElementById('language-selector')?.addEventListener('change', function() {
    const langId = this.value;
    const presets = {
        '62': { mode: 'java', code: 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, Java!");\n    }\n}' },
        '63': { mode: 'javascript', code: "console.log('Hello, JS!');" },
        '54': { mode: 'cpp', code: '#include <iostream>\nint main() {\n    std::cout << "Hello, C++!";\n    return 0;\n}' },
        '71': { mode: 'python', code: "print('Hello, Python!')" }
    };

    const config = presets[langId] || presets['71'];
    monaco.editor.setModelLanguage(state.monacoEditor.getModel(), config.mode);
    state.monacoEditor.setValue(config.code);
});

function getCsrfHeader() {
    const header = document.querySelector('meta[name="_csrf_header"]');
    const token = document.querySelector('meta[name="_csrf"]');
    return (header && token) ? { [header.content]: token.content } : {};
}

/** @description 코드 실행 요청 */
function runCode() {
    const consoleDiv = document.getElementById('output-console');
    consoleDiv.innerText = "실행 중입니다...";

    fetch('/api/interpreter/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getCsrfHeader() },
        body: JSON.stringify({
            code: state.monacoEditor.getValue(),
            languageId: document.getElementById('language-selector').value
        })
    })
        .then(res => res.json())
        .then(data => { consoleDiv.innerText = data.output; })
        .catch(err => { consoleDiv.innerText = "에러 발생: " + err; });
}

/* =========================================
   5. 스마트 퀴즈 시스템 (채점 및 오답 확인 로직)
   ========================================= */

/** @description 퀴즈 데이터 페칭 및 초기화 */
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
            showStep('start');
        })
        .catch(err => alert("⚠️ " + err.message));
}

function startQuizLogic() {
    if (!state.quizData?.questions?.length) return alert("퀴즈 데이터 오류");
    showStep('question');
    renderQuestion();
}

/** @description 문항 렌더링 및 UI 상태 초기화 */
function renderQuestion() {
    state.isGraded = false; // 채점 상태 리셋
    const question = state.quizData.questions[state.currentQIndex];
    const total = state.quizData.questions.length;

    document.getElementById('curr-q-idx').innerText = state.currentQIndex + 1;
    document.getElementById('question-content').innerText = question.content;

    const expText = question.explanation ? question.explanation : "별도의 해설이 없습니다.";
    document.getElementById('explanation-text').innerText = expText; // HTML 태그 허용하려면 innerHTML
    document.getElementById('explanation-area').style.display = 'none'; // 숨김 상태로 시작

    const container = document.getElementById('options-container');
    container.innerHTML = '';
    container.classList.remove('graded');

    question.options.forEach(opt => {
        const btn = document.createElement('div');
        btn.className = 'option-item';
        btn.innerText = opt.content;
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

/** @description 제출 버튼 클릭 시 채점 또는 다음 단계 진행 */
function handleQuizAction() {
    if (!state.isGraded) checkAnswerLocally();
    else nextQuestion();
}

function checkAnswerLocally() {
    const question = state.quizData.questions[state.currentQIndex];
    const selectedBtn = document.querySelector('.option-item.selected');
    if (!selectedBtn) return;

    state.isGraded = true;
    const container = document.getElementById('options-container');
    container.classList.add('graded');

    // 사용자가 선택한 답의 ID (비교를 위해 문자열 변환)
    const selectedOptionId = String(state.userAnswers.find(a => a.questionId === question.questionId).optionId);
    const allOptions = document.querySelectorAll('.option-item');

    allOptions.forEach((btn, idx) => {
        const opt = question.options[idx];

        // 🔥 [해결 포인트] 'T', 't', 'true', true 모두 정답으로 인정하는 정규화 로직
        const rawVal = opt.isCorrect || opt.is_correct || opt.correct;
        const isActuallyCorrect = (
            String(rawVal).trim().toUpperCase() === 'T' ||
            String(rawVal).trim().toUpperCase() === 'Y' ||
            rawVal === true ||
            rawVal === 1
        );

        // 1. 진짜 정답인 경우 (초록색 테두리)
        if (isActuallyCorrect) {
            btn.classList.add('correct');
        }

        // 2. 내가 선택했는데 틀린 경우 (빨간색 테두리)
        if (String(opt.optionId) === selectedOptionId && !isActuallyCorrect) {
            btn.classList.add('wrong');
        }

        // 3. 정답 카운트 (100점 환산용)
        if (String(opt.optionId) === selectedOptionId && isActuallyCorrect) {
            state.currentCorrectCount++;
        }
    });

    const explanationArea = document.getElementById('explanation-area');
    if (explanationArea) {
        explanationArea.style.display = 'block';
    }

    // 버튼 텍스트 변경 (결과 보기 / 다음 문제)
    const nextBtn = document.getElementById('btn-next-question');
    const isLast = state.currentQIndex === state.quizData.questions.length - 1;
    nextBtn.innerText = isLast ? '결과 보기' : '다음 문제';
    nextBtn.style.backgroundColor = isLast ? "#00c471" : "#333";
}

function nextQuestion() {
    if (state.currentQIndex === state.quizData.questions.length - 1) submitQuizFinal();
    else {
        state.currentQIndex++;
        renderQuestion();
    }
}

function showQuizResultUI() {
    const btn = document.querySelector('#quiz-step-result button'); // 결과 화면의 버튼

    // HTML에 hidden input으로 박혀있는 파이널 퀴즈 ID 가져오기
    const finalQuizIdElement = document.getElementById('final-quiz-id');
    const finalQuizId = finalQuizIdElement ? finalQuizIdElement.value : null;

    // 다음 챕터도 없고, 파이널 퀴즈가 대기 중이라면? (현재 푸는 게 파이널이 아님)
    if (!state.nextChapterId && finalQuizId && state.quizData.type !== 'FINAL') {
        btn.innerText = "파이널 퀴즈 풀기";
        btn.style.backgroundColor = "#ff6b6b"; // 빨간색으로 강조
    }
    // 다음 챕터도 없고, (파이널 퀴즈도 없거나 OR 이미 파이널을 푼 경우) -> 완강
    else if (!state.nextChapterId && (!finalQuizId || state.quizData.type === 'FINAL')) {
        btn.innerText = "수강 완료 (메인으로)";
        btn.style.backgroundColor = "#333";
    }
    // 다음 챕터가 있으면
    else {
        btn.innerText = "다음 강의 보기";
        btn.style.backgroundColor = "#333";
    }
}


/** @description 서버 기록 전송 및 100점 만점 결과 도출 */
function submitQuizFinal() {
    fetch('/api/quiz/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getCsrfHeader() },
        body: JSON.stringify({ quizId: state.quizData.quizId, answers: state.userAnswers })
    })
        .then(res => res.json())
        .then(result => {
            // 맞춘 개수 비율 기반 100점 만점 계산
            const ratioScore = Math.round((state.currentCorrectCount / state.quizData.questions.length) * 100);
            renderFinalResult(ratioScore, result.isPassed);
            showQuizResultUI();
            showStep('result');
        })
        .catch(() => alert("결과 전송 중 오류 발생"));
}

function renderFinalResult(score, isPassed) {
    document.getElementById('result-score').innerText = score;
}

function showStep(stepName) {
    document.querySelectorAll('.quiz-step').forEach(el => el.style.display = 'none');
    const target = document.getElementById(`quiz-step-${stepName}`);
    if (target) target.style.display = (stepName === 'question') ? 'block' : 'flex';
}

/** @description 퀴즈 종료 후 다음 챕터로 이동 */
function goToNextChapter() {
    // 이동할 경로 계산
    const nextChapterId = state.nextChapterId;
    const finalQuizIdElement = document.getElementById('final-quiz-id');
    const finalQuizId = finalQuizIdElement ? finalQuizIdElement.value : null;

    // 다음 영상(챕터)이 있으면 -> 영상으로 이동
    if (nextChapterId) {
        location.href = `/course/play?courseId=${state.courseId}&chapterId=${nextChapterId}`;
        return;
    }

    // 다음 영상은 없는데, '파이널 퀴즈'가 있고, 지금 푸는 게 파이널이 아니라면?
    if (finalQuizId && (!state.quizData || state.quizData.type !== 'FINAL')) {
        playContent(finalQuizId, 'QUIZ');
        return;
    }

    // 다음 영상도 없고, (파이널도 없거나 이미 다 품) -> 완강
    if (!nextChapterId && (!finalQuizId || state.quizData?.type === 'FINAL')) {
        alert("모든 강의와 평가를 완료했습니다! 수고하셨습니다. 🎉");
        location.href = `/course/detail?courseId=${state.courseId}`;
        return;
    }

/* =========================================
   자료실 기능
   ========================================= */

// 데이터 가져와서 그리기
function loadResources() {
    const listContainer = document.getElementById('resource-list');
    const emptyMsg = document.getElementById('no-resource-msg');

    // 초기화 (기존 목록 지우기)
    listContainer.innerHTML = '';
    emptyMsg.style.display = 'none';

    // 현재 코스 ID로 요청
    fetch(`/api/resources?courseId=${state.courseId}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
    })
        .then(res => {
            if (!res.ok) throw new Error("자료실 로딩 실패");
            return res.json();
        })
        .then(data => {
            // 데이터가 없으면 '없음' 메시지 표시
            if (!data || data.length === 0) {
                emptyMsg.style.display = 'block';
                return;
            }

            // 데이터가 있으면 리스트 만들기
            data.forEach(item => {
                const li = document.createElement('li');
                li.className = 'resource-item';

                // 1. 파일 타입 대문자로 통일 (DB에 'pdf', 'PDF' 섞여 있을 수 있으므로)
                const typeStr = (item.fileType || 'FILE').toUpperCase();

                // 2. 타입에 따라 적용할 클래스 결정
                let badgeClass = 'badge-default'; // 기본값 (회색)

                if (typeStr === 'PDF') {
                    badgeClass = 'badge-pdf';     // 붉은색
                } else if (typeStr === 'ZIP') {
                    badgeClass = 'badge-zip';     // 푸른색
                }

                // 3. HTML 조립 (클래스 변수 적용)
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
}

