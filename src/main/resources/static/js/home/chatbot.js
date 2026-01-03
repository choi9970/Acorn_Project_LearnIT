// =====================================================
// ✅ LearnIT Chatbot (FINAL + PERSIST + HOME PIN FIX)
// - 페이지 이동 후에도: (1) 대화 내용 유지 (2) 창 열림 상태 유지
// - sessionStorage 사용: 같은 탭/브라우저 세션 내 유지, 브라우저 종료 시 초기화
// - ✅ 강의 찾기(COURSE_RECOMMEND) 중에는 "🏠 처음으로" 버튼이 항상 '진짜로' 맨 아래에 유지
//   → 메시지/카드는 HOME 메뉴 "위"로 insertBefore 해서 HOME가 절대 밀리지 않게 처리
// =====================================================

// ===============================
// ✅ DOM
// ===============================
const chatbotToggle = document.getElementById("chatbot-toggle");
const chatbotWindow = document.getElementById("chatbot-window");
const chatbotClose = document.getElementById("chatbot-close");
const chatbotBody = document.getElementById("chatbot-body");
const chatbotInput = document.getElementById("chatbot-input");
const chatbotSend = document.getElementById("chatbot-send");

// ===============================
// ✅ 전역값 (서버 주입) 방어
// ===============================
window.IS_LOGGED_IN =
  typeof window.IS_LOGGED_IN === "boolean" ? window.IS_LOGGED_IN : false;

window.CSRF_HEADER = window.CSRF_HEADER || "X-CSRF-TOKEN";
window.CSRF_TOKEN = window.CSRF_TOKEN || "";

// QNA: 강의 상세에서만 주입되면 Number, 아니면 null
window.DEFAULT_QNA_COURSE_ID =
  window.DEFAULT_QNA_COURSE_ID !== undefined &&
  window.DEFAULT_QNA_COURSE_ID !== null &&
  window.DEFAULT_QNA_COURSE_ID !== ""
    ? Number(window.DEFAULT_QNA_COURSE_ID)
    : null;

// ===============================
// ✅ 저장 키
// ===============================
const CHATBOT_STORAGE_KEY = "learnit_chatbot_state_v1";

// ===============================
// ✅ 상태
// ===============================
const state = {
  mode: "IDLE", // IDLE | COURSE_RECOMMEND | HUMAN_WAIT_CONTENT
  chatSessionId: null, // Python chat-agent 세션 ID
};

// ===============================
// ✅ 메뉴
// ===============================
const MAIN_MENU = [
  { key: "COURSE_FIND", label: "📚 강의 찾기" },
  { key: "MY_LEARNING", label: "▶️ 내 학습 현황" },
  { key: "PAYMENT", label: "💳 결제내역확인" },
  { key: "NOTICE", label: "📢 공지 & 이벤트" },
  { key: "HUMAN", label: "🧑‍💼 상담원 연결" },
];

// ===============================
// ✅ Persistence (save/restore)
// ===============================
function saveChatbotState() {
  try {
    const payload = {
      html: chatbotBody ? chatbotBody.innerHTML : "",
      mode: state.mode,
      chatSessionId: state.chatSessionId,
      isOpen: chatbotWindow ? chatbotWindow.style.display === "flex" : false,
    };
    sessionStorage.setItem(CHATBOT_STORAGE_KEY, JSON.stringify(payload));
  } catch (e) {
    // ignore
  }
}

function restoreChatbotState() {
  const raw = sessionStorage.getItem(CHATBOT_STORAGE_KEY);
  if (!raw) return false;

  try {
    const saved = JSON.parse(raw);

    if (chatbotBody && typeof saved.html === "string") {
      chatbotBody.innerHTML = saved.html;
      chatbotBody.scrollTop = chatbotBody.scrollHeight;
    }

    state.mode = saved.mode || "IDLE";
    state.chatSessionId = saved.chatSessionId || null;

    return true;
  } catch (e) {
    return false;
  }
}

// ===============================
// ✅ UI 업데이트
// ===============================
function openChatbotUI() {
  chatbotWindow.style.display = "flex";
  chatbotWindow.style.flexDirection = "column";
  chatbotToggle.style.opacity = "0";
  chatbotToggle.style.pointerEvents = "none";
}

function closeChatbotUI() {
  chatbotWindow.style.display = "none";
  chatbotToggle.style.opacity = "1";
  chatbotToggle.style.pointerEvents = "auto";
}

// ===============================
// ✅ 메뉴/삽입 유틸 (HOME 고정 핵심)
// ===============================

// quick-menu(버튼 영역) "전부" 제거
function removeExistingMenu() {
  chatbotBody.querySelectorAll(".quick-menu").forEach((m) => m.remove());
  saveChatbotState();
}

// 강의찾기 모드에서 "HOME 메뉴(quick-menu)" 찾기
function getPinnedHomeMenu() {
  const menus = Array.from(chatbotBody.querySelectorAll(".quick-menu"));
  for (let i = menus.length - 1; i >= 0; i--) {
    if (menus[i].querySelector('.quick-btn[data-action="HOME"]')) return menus[i];
  }
  return null;
}

// ✅ 강의찾기 모드에서는 'HOME 위'로 insertBefore 해서 HOME가 절대 밀리지 않게 함
function appendInChatFlow(el) {
  if (state.mode === "COURSE_RECOMMEND") {
    const homeMenu = getPinnedHomeMenu();
    if (homeMenu) chatbotBody.insertBefore(el, homeMenu);
    else chatbotBody.appendChild(el);
  } else {
    chatbotBody.appendChild(el);
  }
  chatbotBody.scrollTop = chatbotBody.scrollHeight;
  saveChatbotState();
}

// ✅ 강의찾기 중 HOME 버튼은 항상 맨 아래에 "하나만" 유지
function keepHomeAtBottom() {
  // 기존 HOME 메뉴 제거(중복 방지)
  chatbotBody.querySelectorAll(".quick-menu").forEach((menu) => {
    const hasHome = menu.querySelector('.quick-btn[data-action="HOME"]');
    if (hasHome) menu.remove();
  });

  // 맨 아래에 HOME 메뉴 생성
  const menuEl = document.createElement("div");
  menuEl.className = "quick-menu";

  const homeBtn = document.createElement("button");
  homeBtn.type = "button";
  homeBtn.className = "quick-btn";
  homeBtn.dataset.action = "HOME";
  homeBtn.textContent = "🏠 처음으로";

  menuEl.appendChild(homeBtn);
  chatbotBody.appendChild(menuEl); // ✅ 무조건 맨 아래

  chatbotBody.scrollTop = chatbotBody.scrollHeight;
  saveChatbotState();
}

// 메뉴 버튼(액션) 붙이기 - 기존 메뉴 제거 후 1개만 유지
function appendMenuButtons(buttons) {
  removeExistingMenu();

  const menuEl = document.createElement("div");
  menuEl.className = "quick-menu";

  buttons.forEach((b) => {
    const el = document.createElement("button");
    el.type = "button";
    el.className = "quick-btn";
    el.dataset.action = b.key;
    el.textContent = b.label;
    menuEl.appendChild(el);
  });

  chatbotBody.appendChild(menuEl);
  chatbotBody.scrollTop = chatbotBody.scrollHeight;
  saveChatbotState();
}

// ✅ 메뉴를 "추가로" 붙이는 버전 (기존 메뉴 삭제 X)
function appendMenuButtonsKeep(buttons) {
  const menuEl = document.createElement("div");
  menuEl.className = "quick-menu";

  buttons.forEach((b) => {
    const el = document.createElement("button");
    el.type = "button";
    el.className = "quick-btn";
    el.dataset.action = b.key;
    el.textContent = b.label;
    menuEl.appendChild(el);
  });

  chatbotBody.appendChild(menuEl);
  chatbotBody.scrollTop = chatbotBody.scrollHeight;
  saveChatbotState();
}

// ✅ 링크 버튼(옵션 B): data-action 없는 a 태그 (JS가 가로채지 않음)
function appendLinkButton({ href, label, target = "_blank" }) {
  const menuEl = document.createElement("div");
  menuEl.className = "quick-menu";

  const a = document.createElement("a");
  a.className = "quick-btn";
  a.href = href;
  a.target = target;
  a.rel = "noopener";
  a.textContent = label;

  menuEl.appendChild(a);
  chatbotBody.appendChild(menuEl);
  chatbotBody.scrollTop = chatbotBody.scrollHeight;
  saveChatbotState();
}

// ===============================
// ✅ UI 유틸 (메시지/카드)
// ===============================
function addBotMessage(text) {
  const el = document.createElement("div");
  el.className = "bot-message";
  el.textContent = text;
  appendInChatFlow(el); // ✅ HOME 위로 들어감(강의찾기 중)
}

function addUserMessage(text) {
  const el = document.createElement("div");
  el.className = "user-message";
  el.textContent = text;
  appendInChatFlow(el); // ✅ HOME 위로 들어감(강의찾기 중)
}

// 입력 잠금/해제
function setChatInputEnabled(enabled) {
  chatbotInput.disabled = !enabled;
  chatbotSend.disabled = !enabled;

  chatbotInput.placeholder = enabled
    ? "메시지를 입력하세요..."
    : "메뉴에서 선택해 주세요.";

  saveChatbotState();
}

function showFollowUpMenu(extraButtons = []) {
  addBotMessage("다음엔 다른 문의가 필요하신가요? 😊");
  appendMenuButtons([...extraButtons, ...MAIN_MENU]);
}

// ===============================
// ✅ 로그인 필요 UI
// ===============================
function showLoginRequired(message, returnUrl) {
  state.mode = "IDLE";
  setChatInputEnabled(false);

  addBotMessage(message || "이 기능은 로그인이 필요해요. 로그인 해주세요.");

  removeExistingMenu();

  const menuEl = document.createElement("div");
  menuEl.className = "quick-menu";

  const loginBtn = document.createElement("button");
  loginBtn.type = "button";
  loginBtn.className = "quick-btn";
  loginBtn.textContent = "🔐 로그인";
  loginBtn.onclick = () => {
    const url = returnUrl
      ? `/login?returnUrl=${encodeURIComponent(returnUrl)}`
      : "/login";
    window.location.href = url;
  };

  const homeBtn = document.createElement("button");
  homeBtn.type = "button";
  homeBtn.className = "quick-btn";
  homeBtn.dataset.action = "HOME";
  homeBtn.textContent = "🏠 처음으로";

  menuEl.appendChild(loginBtn);
  menuEl.appendChild(homeBtn);

  chatbotBody.appendChild(menuEl);
  chatbotBody.scrollTop = chatbotBody.scrollHeight;

  saveChatbotState();
}

// ===============================
// ✅ 메인 메뉴
// ===============================
function showMainMenu() {
  state.mode = "IDLE";
  setChatInputEnabled(false);

  removeExistingMenu();
  addBotMessage("원하시는 항목을 선택해 주세요 😊");
  appendMenuButtons(MAIN_MENU);
}

// ===============================
// ✅ 강의 카드
// ===============================
function renderCourseCards(items) {
  const wrap = document.createElement("div");
  wrap.className = "course-cards";

  items.forEach((c) => {
    const card = document.createElement("div");
    card.className = "course-card";

    const title = document.createElement("div");
    title.className = "course-title";
    title.textContent = c.title || "(제목 없음)";

    const price = document.createElement("div");
    price.className = "course-price";
    const p = Number(c.price || 0);
    price.textContent = p === 0 ? "무료" : `${p.toLocaleString()}원`;

    const desc = document.createElement("div");
    desc.className = "course-desc";
    desc.textContent = c.description || "";

    const btn = document.createElement("a");
    btn.className = "course-btn";
    btn.href = c.detailUrl || "#";
    btn.target = "_blank";
    btn.rel = "noopener";
    btn.textContent = "바로 보기";

    card.appendChild(title);
    card.appendChild(price);
    card.appendChild(desc);
    card.appendChild(btn);

    wrap.appendChild(card);
  });

  return wrap;
}

function addBotCards(items) {
  const cardsEl = renderCourseCards(items);
  appendInChatFlow(cardsEl); // ✅ HOME 위로 들어감(강의찾기 중)
}

// ===============================
// ✅ 메뉴 처리
// ===============================
function handleAction(actionKey) {
  if (actionKey === "HOME") {
    showMainMenu();
    return;
  }

  switch (actionKey) {
    // ---------------------------------
    // 📚 강의 찾기: HOME 항상 맨 아래 고정
    // ---------------------------------
    case "COURSE_FIND": {
      state.mode = "COURSE_RECOMMEND";
      state.chatSessionId = null;
      setChatInputEnabled(true);

      // 메뉴 정리 후 안내
      removeExistingMenu();
      addBotMessage(
        "원하시는 강의/목표를 채팅으로 적어주세요 😊 예) 자바 입문, 스프링 프로젝트, SQL 기초"
      );

      // ✅ HOME 생성(맨 아래) → 이후 메시지/카드는 insertBefore로 HOME 위로만 들어감
      keepHomeAtBottom();
      break;
    }

    // ---------------------------------
    // ▶️ 내 학습 현황: 로그인 필요 + 링크만 보여주기(옵션 B)
    // ---------------------------------
    case "MY_LEARNING": {
      setChatInputEnabled(false);

      if (!window.IS_LOGGED_IN) {
        showLoginRequired(
          "내학습 현황은 로그인이 필요해요. 로그인 해주세요.",
          "/mypage/courses"
        );
        return;
      }

      removeExistingMenu();
      addBotMessage("학습 현황은 아래에서 확인 할수 있습니다.");
      appendLinkButton({ href: "/mypage/courses", label: "📈 학습현황 확인", target: "_blank" });

      addBotMessage("다음엔 다른 문의가 필요하신가요? 😊");
      appendMenuButtonsKeep(MAIN_MENU);
      break;
    }

    // ---------------------------------
    // 💳 결제내역확인: 로그인 필요 + 링크만 보여주기(옵션 B)
    // ---------------------------------
    case "PAYMENT": {
      setChatInputEnabled(false);

      if (!window.IS_LOGGED_IN) {
        showLoginRequired(
          "결제내역 확인은 로그인이 필요해요. 로그인 해주세요.",
          "/mypage/purchase"
        );
        return;
      }

      removeExistingMenu();
      addBotMessage("결제내역확인은 아래에서 확인 할수 있습니다.");
      appendLinkButton({ href: "/mypage/purchase", label: "💳 결제내역확인", target: "_blank" });

      addBotMessage("다음엔 다른 문의가 필요하신가요? 😊");
      appendMenuButtonsKeep(MAIN_MENU);
      break;
    }

    // ---------------------------------
    // 📢 공지 & 이벤트: 로그인 상관없이 링크만(옵션 B)
    // ---------------------------------
    case "NOTICE": {
      setChatInputEnabled(false);

      removeExistingMenu();
      addBotMessage("공지 & 이벤트는 아래에서 확인 할수 있습니다.");
      appendLinkButton({ href: "/notice", label: "📢 공지 & 이벤트", target: "_self" });

      addBotMessage("다음엔 다른 문의가 필요하신가요? 😊");
      appendMenuButtonsKeep(MAIN_MENU);
      break;
    }

    // ---------------------------------
    // 🧑‍💼 상담원 연결: 로그인 필요
    // ---------------------------------
    case "HUMAN": {
      if (!window.IS_LOGGED_IN) {
        showLoginRequired("상담원 문의는 로그인이 필요해요. 로그인 해주세요.");
        return;
      }

      state.mode = "HUMAN_WAIT_CONTENT";
      setChatInputEnabled(true);

      removeExistingMenu();
      addBotMessage("문의 내용을 챗봇에 작성해 주세요. 확인 후 연락 드리겠습니다 😊");
      appendMenuButtons([{ key: "HOME", label: "🏠 처음으로" }]);
      break;
    }

    default: {
      addBotMessage("원하시는 항목을 선택해 주세요 😊");
      appendMenuButtons(MAIN_MENU);
      break;
    }
  }

  saveChatbotState();
}

// ===============================
// ✅ 입력 전송
// ===============================
async function sendMessage() {
  const msg = chatbotInput.value.trim();
  if (!msg) return;

  addUserMessage(msg);
  chatbotInput.value = "";
  chatbotInput.focus();

  // ===============================
  // 📚 강의 찾기: 챗봇(chat-agent) 추천 대화
  // ===============================
  if (state.mode === "COURSE_RECOMMEND") {
    try {
      const headers = { "Content-Type": "application/json" };
      if (window.CSRF_TOKEN) headers[window.CSRF_HEADER] = window.CSRF_TOKEN;

      const res = await fetch("/api/chatbot/chat", {
        method: "POST",
        headers,
        body: JSON.stringify({
          message: msg,
          sessionId: state.chatSessionId,
        }),
      });

      let resp = null;
      try {
        resp = await res.json();
      } catch (e) {
        resp = null;
      }

      if (!res.ok) {
        addBotMessage("추천을 불러오는 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.");
        // HOME가 있어야 insertBefore가 안정적 → 혹시 없으면 재생성
        if (!getPinnedHomeMenu()) keepHomeAtBottom();
        return;
      }

      if (resp?.sessionId) state.chatSessionId = resp.sessionId;

      addBotMessage(resp?.reply || "추천 결과를 가져왔어요!");

      if (Array.isArray(resp?.items) && resp.items.length > 0) {
        addBotCards(resp.items);
      }

      // HOME가 있어야 insertBefore가 안정적 → 혹시 없으면 재생성
      if (!getPinnedHomeMenu()) keepHomeAtBottom();
      return;
    } catch (e) {
      addBotMessage("추천을 불러오는 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.");
      if (!getPinnedHomeMenu()) keepHomeAtBottom();
      return;
    } finally {
      saveChatbotState();
    }
  }

  // ---------------------------------
  // 🧑‍💼 상담원 문의: DB 저장
  // ---------------------------------
  if (state.mode === "HUMAN_WAIT_CONTENT") {
    try {
      const headers = { "Content-Type": "application/json" };
      if (window.CSRF_TOKEN) headers[window.CSRF_HEADER] = window.CSRF_TOKEN;

      const payload = {
        courseId: window.DEFAULT_QNA_COURSE_ID, // null 가능(서버/DB도 허용해야 함)
        content: msg,
      };

      const res = await fetch("/api/chatbot/qna", {
        method: "POST",
        headers,
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error("qna failed");

      addBotMessage("접수되었습니다. 확인 후 연락 드리겠습니다 😊");

      state.mode = "IDLE";
      setChatInputEnabled(false);
      showFollowUpMenu();
      saveChatbotState();
      return;
    } catch (e) {
      addBotMessage("죄송해요. 접수 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.");
      saveChatbotState();
      return;
    }
  }

  // 기본 모드(원칙상 채팅 비활성)
  addBotMessage("메뉴에서 항목을 선택해 주세요 😊");
  saveChatbotState();
}

// ===============================
// ✅ 이벤트 리스너
// ===============================

// 페이지 로드 시: "열려있던 상태면 자동으로 열고 복원"
document.addEventListener("DOMContentLoaded", () => {
  const raw = sessionStorage.getItem(CHATBOT_STORAGE_KEY);
  if (!raw) return;

  try {
    const saved = JSON.parse(raw);
    if (saved && saved.isOpen) {
      openChatbotUI();
      restoreChatbotState();

      if (state.mode === "COURSE_RECOMMEND" || state.mode === "HUMAN_WAIT_CONTENT") {
        setChatInputEnabled(true);
      } else {
        setChatInputEnabled(false);
      }

      // ✅ 복원 후 강의찾기면 HOME가 없을 수도 있으니 보정
      if (state.mode === "COURSE_RECOMMEND" && !getPinnedHomeMenu()) {
        keepHomeAtBottom();
      }

      chatbotBody.scrollTop = chatbotBody.scrollHeight;
    }
  } catch (e) {
    // ignore
  }
});

// 열기(수동)
chatbotToggle.addEventListener("click", () => {
  openChatbotUI();

  const restored = restoreChatbotState();
  if (!restored) {
    setChatInputEnabled(false);
    showMainMenu();
  } else {
    if (state.mode === "COURSE_RECOMMEND" || state.mode === "HUMAN_WAIT_CONTENT") {
      setChatInputEnabled(true);
    } else {
      setChatInputEnabled(false);
    }

    // ✅ 열기 후 강의찾기면 HOME 보정
    if (state.mode === "COURSE_RECOMMEND" && !getPinnedHomeMenu()) {
      keepHomeAtBottom();
    }

    chatbotBody.scrollTop = chatbotBody.scrollHeight;
  }

  saveChatbotState();
});

// 닫기
chatbotClose.addEventListener("click", () => {
  closeChatbotUI();
  saveChatbotState();
});

// 전송
chatbotSend.addEventListener("click", () => sendMessage());
chatbotInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") sendMessage();
});

// quick 버튼 클릭 (이벤트 위임)
// ✅ data-action 있는 버튼만 handleAction 실행
// ✅ 링크(a.quick-btn)는 data-action이 없으므로 그대로 href 이동
document.addEventListener("click", (e) => {
  const btn = e.target.closest(".quick-btn");
  if (!btn) return;

  const actionKey = btn.dataset.action;

  // 링크(a) 또는 data-action 없는 경우는 JS가 가로채지 않음
  if (!actionKey) return;

  // 액션 버튼만 처리
  removeExistingMenu();
  addUserMessage(btn.textContent);

  handleAction(actionKey);
  saveChatbotState();
});
