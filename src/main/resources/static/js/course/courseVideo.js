// HTML hidden input에서 정보 가져오기 (URL, CourseID, ChapterID)
const videoInput = document.getElementById('video-url');
const courseInput = document.getElementById('course-id');
const chapterInput = document.getElementById('chapter-id');

const dbVideoUrl = videoInput ? videoInput.value : null;
const currentCourseId = courseInput ? courseInput.value : null;
const currentChapterId = chapterInput ? chapterInput.value : null;

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
            'onStateChange': onPlayerStateChange
        }
    });
};

// 상태 변화 감지
function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.PAUSED) {
        saveCurrentTime();
    }
    if (event.data === YT.PlayerState.ENDED) {
        localStorage.removeItem(storageKey);
    }
}

// 로컬 스토리지에 시간 저장 (이어보기용)
function saveCurrentTime() {
    if (!player || typeof player.getCurrentTime !== 'function') return;
    const current = Math.floor(player.getCurrentTime());
    localStorage.setItem(storageKey, current);
}

// 서버로 진도율 전송 (DB 저장용)
function saveProgressToServer(time) {
    if (!currentCourseId || !currentChapterId) return;

    const payload = { playTime: time };
    const url = `/course/${currentCourseId}/video/${currentChapterId}/log`;

    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
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

// API가 먼저 로드되었을 경우를 대비해 수동 실행
if (window.YT && window.YT.Player && typeof window.YT.Player === 'function') {
    window.onYouTubeIframeAPIReady();
}