// =====================================================
// course-detail-review.js (FULL, COPY-PASTE)
// UX 요구사항:
// - 최초 진입: 별점 5, textarea 빈값, 버튼 "등록" (자동 edit 진입 금지)
// - 등록 시 409(이미 리뷰 있음): 경고만 표시, 폼 상태 유지(등록 모드 유지)
// - 사용자가 리스트에서 "수정" 눌렀을 때만: 기존 내용 채우고 버튼 "수정"
// - 삭제/수정 API 호출 + CSRF + /login 리다이렉트 감지
// =====================================================

let REVIEW_MODE = "create"; // "create" | "edit"
let EDIT_REVIEW_ID = null;

// ================================
// courseId
// ================================
function getCourseId() {
  const el = document.getElementById("courseId");
  if (el && el.value) return String(el.value).trim();

  const params = new URLSearchParams(window.location.search);
  const q = params.get("courseId");
  return q && q.trim() ? q.trim() : null;
}

// ================================
// CSRF (meta)
// ================================
function getCsrf() {
  const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
  const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content") || "";
  return { token, header };
}

// ================================
// XSS 방어(최소)
// ================================
function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

// ================================
// date parse
// ================================
function toDateSafe(v) {
  if (!v) return null;

  if (typeof v === "string") {
    const d = new Date(v);
    return isNaN(d.getTime()) ? null : d;
  }

  if (Array.isArray(v)) {
    const [y, m, d, hh = 0, mm = 0, ss = 0] = v;
    const dt = new Date(y, m - 1, d, hh, mm, ss);
    return isNaN(dt.getTime()) ? null : dt;
  }

  return null;
}

// ================================
// UI message
// ================================
function setMessage(msg, type = "info") {
  const el = document.getElementById("reviewMessage");
  if (!el) return;

  el.textContent = msg || "";
  el.className = "";
  if (msg) el.classList.add("review-message", `is-${type}`);
}

// ================================
// fetch wrapper
// - 302(/login) 리다이렉트 감지
// ================================
async function apiFetch(url, options = {}) {
  const { token, header } = getCsrf();

  const headers = {
    Accept: "application/json",
    ...(options.headers || {}),
    ...(token && header ? { [header]: token } : {}),
  };

  let res;
  try {
    res = await fetch(url, {
      credentials: "same-origin",
      ...options,
      headers,
    });
  } catch (e) {
    console.error("[apiFetch] network error", e);
    return { ok: false, res: null, data: null, text: "", redirectedToLogin: false };
  }

  const redirectedToLogin = Boolean(res.redirected && (res.url || "").includes("/login"));
  const text = await res.text();

  let data = null;
  try { data = JSON.parse(text); } catch {}

  return { ok: res.ok && !redirectedToLogin, res, data, text, redirectedToLogin };
}

// ================================
// mode helpers
// ================================
function setFormModeCreate() {
  REVIEW_MODE = "create";
  EDIT_REVIEW_ID = null;

  const btn = document.querySelector("#reviewForm button[type='submit']");
  if (btn) btn.textContent = "수강평 등록";

  const ratingEl = document.getElementById("rating");
  const commentEl = document.getElementById("comment");
  if (ratingEl) ratingEl.value = "5";
  if (commentEl) commentEl.value = "";
}

function setFormModeEdit(review) {
  REVIEW_MODE = "edit";
  EDIT_REVIEW_ID = review?.reviewId ?? null;

  const btn = document.querySelector("#reviewForm button[type='submit']");
  if (btn) btn.textContent = "수강평 수정";

  const ratingEl = document.getElementById("rating");
  const commentEl = document.getElementById("comment");
  if (ratingEl) ratingEl.value = String(review?.rating ?? "5");
  if (commentEl) commentEl.value = String(review?.content ?? "");

  document.getElementById("reviewFormWrapper")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

// ================================
// render reviews
// ================================
function renderReviews(reviews) {
  const list = document.getElementById("reviewList");
  if (!list) return;

  list.innerHTML = "";

  if (!reviews || reviews.length === 0) {
    list.innerHTML = "<p>아직 등록된 수강평이 없습니다.</p>";
    return;
  }

  const frag = document.createDocumentFragment();

  reviews.forEach((r) => {
    const isMine = r.mine === true || r.isMine === true;

    const rating = Number(r.rating ?? 0);
    const safeRating = Math.min(5, Math.max(0, isNaN(rating) ? 0 : rating));

    const nickname = (r.nickname ?? "익명").toString();
    const content = (r.content ?? "").toString();

    const d = toDateSafe(r.updatedAt ?? r.createdAt);
    const dateText = d ? d.toLocaleDateString() : "";

    const card = document.createElement("div");
    card.className = "review-card";
    card.setAttribute("data-review-id", r.reviewId ?? "");

    card.innerHTML = `
      <div class="review-top">
        <b>${escapeHtml(nickname)}</b>
        <span>${"★".repeat(safeRating)}${"☆".repeat(5 - safeRating)}</span>
      </div>
      <div class="review-date">${escapeHtml(dateText)}</div>
      <p>${escapeHtml(content)}</p>
      ${
        isMine
          ? `
        <div class="review-actions">
          <button type="button" data-action="edit" data-id="${r.reviewId}">수정</button>
          <button type="button" data-action="delete" data-id="${r.reviewId}">삭제</button>
        </div>`
          : ""
      }
    `;

    frag.appendChild(card);
  });

  list.appendChild(frag);
}

// ================================
// load list + summary
// return { reviews }
// ================================
async function loadReviews() {
  const courseId = getCourseId();
  if (!courseId) return { reviews: [] };

  const listRes = await apiFetch(`/api/reviews?courseId=${encodeURIComponent(courseId)}`);

  if (listRes.redirectedToLogin) {
    setMessage("로그인이 필요합니다. 로그인 후 다시 시도해 주세요.", "warn");
    return { reviews: [] };
  }

  const reviews = Array.isArray(listRes.data)
    ? listRes.data
    : (listRes.data?.items ?? listRes.data?.data ?? []);

  renderReviews(reviews);

  const summary = await apiFetch(`/api/reviews/summary?courseId=${encodeURIComponent(courseId)}`);
  if (summary.ok && summary.data) {
    const avg = Number(summary.data.average ?? 0);
    const count = Number(summary.data.count ?? 0);

    const avgEl = document.getElementById("avgRating");
    const cntEl = document.getElementById("reviewCount");
    const starEl = document.getElementById("avgStars");

    if (avgEl) avgEl.innerText = (isNaN(avg) ? 0 : avg).toFixed(1);
    if (cntEl) cntEl.innerText = `${isNaN(count) ? 0 : count}개의 수강평`;
    if (starEl) {
      const rounded = Math.round(isNaN(avg) ? 0 : avg);
      starEl.innerText = "★".repeat(rounded) + "☆".repeat(5 - rounded);
    }
  }

  return { reviews };
}

// ================================
// submit create/update
// ================================
function bindFormSubmit() {
  const form = document.getElementById("reviewForm");
  if (!form) return;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    e.stopPropagation();

    setMessage("");

    const courseId = getCourseId();
    if (!courseId) {
      setMessage("courseId를 찾을 수 없습니다.", "error");
      return;
    }

    const rating = Number(document.getElementById("rating")?.value ?? 0);
    const content = (document.getElementById("comment")?.value ?? "").trim();

    if (!content) {
      setMessage("후기를 입력해 주세요.", "error");
      return;
    }

    // ✅ CREATE
    if (REVIEW_MODE === "create") {
      const res = await apiFetch(`/api/reviews?courseId=${encodeURIComponent(courseId)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ rating: isNaN(rating) ? 0 : rating, content }),
      });

      if (res.redirectedToLogin) {
        setMessage("로그인이 필요합니다. 로그인 후 다시 시도해 주세요.", "warn");
        return;
      }

      if (!res.ok) {
        if (res.res && res.res.status === 409) {
          // ✅ 요구사항: 409이면 경고만, 폼은 그대로(등록 유지/내용 유지)
          setMessage("이미 작성한 수강평이 있어요. 목록에서 ‘수정’으로 변경해 주세요.", "warn");
          return;
        }
        if (res.res && (res.res.status === 401 || res.res.status === 403)) {
          setMessage("로그인 후 이용 가능합니다.", "warn");
          return;
        }
        setMessage(`등록 실패 (HTTP ${res.res ? res.res.status : "?"})`, "error");
        return;
      }

      setMessage("수강평이 등록되었습니다.", "ok");
      await loadReviews();

      // ✅ 요구사항: 등록 성공 후에도 자동 edit 진입 X
      // 폼은 등록 모드 그대로 두되, textarea 비우고 싶으면 아래 주석 해제
      // setFormModeCreate();
      return;
    }

    // ✅ EDIT
    if (REVIEW_MODE === "edit") {
      if (!EDIT_REVIEW_ID) {
        setMessage("수정할 리뷰를 찾지 못했습니다.", "error");
        return;
      }

      const res = await apiFetch(`/api/reviews/${encodeURIComponent(EDIT_REVIEW_ID)}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ rating: isNaN(rating) ? 0 : rating, content }),
      });

      if (res.redirectedToLogin) {
        setMessage("로그인이 필요합니다. 로그인 후 다시 시도해 주세요.", "warn");
        return;
      }

      if (!res.ok) {
        if (res.res && (res.res.status === 401 || res.res.status === 403)) {
          setMessage("로그인 후 이용 가능합니다.", "warn");
          return;
        }
        setMessage(`수정 실패 (HTTP ${res.res ? res.res.status : "?"})`, "error");
        return;
      }

      setMessage("수강평이 수정되었습니다.", "ok");
      await loadReviews();
      return;
    }
  });
}

// ================================
// list actions (edit/delete)
// ================================
function bindListActions() {
  const list = document.getElementById("reviewList");
  if (!list) return;

  list.addEventListener("click", async (e) => {
    const btn = e.target?.closest?.("button[data-action]");
    if (!btn) return;

    const action = btn.getAttribute("data-action");
    const id = btn.getAttribute("data-id");
    if (!id) return;

    // ✅ EDIT (사용자가 눌렀을 때만 기존 내용 채우기)
    if (action === "edit") {
      setMessage(""); // 문구는 원하면 여기서만 띄워도 됨

      const { reviews } = await loadReviews();
      const target = Array.isArray(reviews) ? reviews.find((r) => Number(r.reviewId) === Number(id)) : null;

      if (!target) {
        setMessage("수정할 리뷰를 찾지 못했습니다.", "error");
        return;
      }

      setFormModeEdit(target);
      return;
    }

    // ✅ DELETE
    if (action === "delete") {
      const ok = confirm("정말 삭제하시겠습니까?");
      if (!ok) return;

      const res = await apiFetch(`/api/reviews/${encodeURIComponent(id)}`, { method: "DELETE" });

      if (res.redirectedToLogin) {
        setMessage("로그인이 필요합니다. 로그인 후 다시 시도해 주세요.", "warn");
        return;
      }

      if (!res.ok) {
        if (res.res && (res.res.status === 401 || res.res.status === 403)) {
          setMessage("로그인 후 이용 가능합니다.", "warn");
          return;
        }
        setMessage(`삭제 실패 (HTTP ${res.res ? res.res.status : "?"})`, "error");
        return;
      }

      setMessage("수강평이 삭제되었습니다.", "ok");
      await loadReviews();

      // 삭제 후엔 다시 등록 모드로 초기화(요구 UX에 자연스러움)
      setFormModeCreate();
      return;
    }
  });
}

// ================================
// init
// ================================
document.addEventListener("DOMContentLoaded", async () => {
  const wrapper = document.getElementById("reviewFormWrapper");
  if (wrapper) wrapper.style.display = window.IS_LOGGED_IN === true ? "" : "none";

  // ✅ 최초 진입은 무조건 등록 모드로 시작
  setFormModeCreate();

  bindFormSubmit();
  bindListActions();
  await loadReviews();
});
