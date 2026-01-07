let __qnaInitialized = false;

function initQnaPanel() {
  if (__qnaInitialized) {
    qnaLoad();
    return;
  }
  __qnaInitialized = true;

  document.getElementById("qna-btn-create")?.addEventListener("click", qnaCreate);
  qnaLoad();
}

async function qnaLoad() {
  const courseId = document.getElementById("course-id")?.value;
  if (!courseId) return;

  const listEl = document.getElementById("qna-list");
  const emptyEl = document.getElementById("qna-empty");
  if (!listEl) return;

  listEl.innerHTML = "";
  if (emptyEl) emptyEl.style.display = "none";

  const res = await fetchWithCsrf(`/api/qna/questions?courseId=${courseId}`, { method: "GET" });
  if (!res.ok) {
    listEl.innerHTML = `<div class="qna-error">Q&A를 불러오지 못했어요.</div>`;
    return;
  }

  const data = await res.json();
  if (!data || data.length === 0) {
    if (emptyEl) emptyEl.style.display = "block";
    return;
  }

  data.forEach((q) => listEl.appendChild(renderQuestion(q)));
}

async function qnaCreate() {
  const courseId = document.getElementById("course-id")?.value;
  const ta = document.getElementById("qna-new-content");
  const hint = document.getElementById("qna-compose-hint");
  if (!courseId || !ta) return;

  const content = (ta.value || "").trim();
  if (!content) {
    if (hint) hint.textContent = "내용을 입력해 주세요.";
    return;
  }
  if (hint) hint.textContent = "";

  const res = await fetchWithCsrf(`/api/qna/questions`, {
    method: "POST",
    body: JSON.stringify({ courseId: Number(courseId), content }),
  });

  if (res.status === 403) {
    if (hint) hint.textContent = "수강중인 회원만 질문을 작성할 수 있어요.";
    return;
  }
  if (!res.ok) {
    if (hint) hint.textContent = "질문 등록에 실패했어요.";
    return;
  }

  ta.value = "";
  await qnaLoad();
}

function renderQuestion(q) {
  const wrap = document.createElement("div");
  wrap.className = "qna-item";
  wrap.dataset.qnaId = q.qnaId;

  const resolvedBadge =
    q.isResolved === "Y"
      ? `<span class="qna-badge resolved">해결</span>`
      : `<span class="qna-badge pending">미해결</span>`;

  const canEditQ = q.canEditQuestion !== undefined ? q.canEditQuestion : !!q.canEditOrDelete;
  const canDelQ = q.canDeleteQuestion !== undefined ? q.canDeleteQuestion : !!q.canEditOrDelete;

  const ownerActions =
    canEditQ || canDelQ
      ? `
        ${canEditQ ? `<button class="qna-link" data-act="q-edit">수정</button>` : ``}
        ${canDelQ ? `<button class="qna-link danger" data-act="q-del">삭제</button>` : ``}
      `
      : ``;

  // ✅ 관리자 답변 버튼
  const adminAnswerBtn = q.canAnswer ? `<button class="qna-btn" data-act="a-open">답변</button>` : ``;

  // ✅ 질문자 댓글 버튼 (원글에만 댓글 달기)
  const commentBtn = q.canComment ? `<button class="qna-btn" data-act="c-open">댓글</button>` : ``;

  // ==============================
  // ✅ 미래 대비: 답변/댓글(자식 포함) 유무 판정
  // - 지금은 사실상 q.answers.length > 0 과 동일
  // - 나중에 a.replies / a.children 생겨도 자동 대응
  // ==============================
  const list = q.answers || [];
  const hasThread = (() => {
    if (!Array.isArray(list) || list.length === 0) return false;
    for (const a of list) {
      if (!a) continue;
      // 삭제 플래그가 있다면 살아있는 것만 체크(필드 없으면 그냥 true로 동작)
      if (a.deleteFlg === undefined || !a.deleteFlg) return true;

      const children = a.replies || a.children || a.comments;
      if (Array.isArray(children) && children.length > 0) return true;
    }
    return false;
  })();

  // ✅ 버튼 + 작성폼을 하나의 블록으로 묶어서
  // "답변이 있으면 아래로", "없으면 기존 위치"로 이동
  const composeBlock = `
    <div class="qna-compose-block" data-role="compose-block">
      <div class="qna-answer-actions">
        ${adminAnswerBtn}
        ${commentBtn}
      </div>

      <!-- ✅ 관리자 답변 작성 -->
      <div class="qna-answer-compose" data-role="a-compose" style="display:none;">
        <textarea class="qna-ta" placeholder="관리자 답변을 입력하세요."></textarea>

        <div class="qna-row">
          ${q.canSetResolved ? `
            <label class="qna-select-wrap">
              <span>해결여부</span>
              <select class="qna-select" data-role="resolved">
                <option value="N" ${q.isResolved === "N" ? "selected" : ""}>미해결</option>
                <option value="Y" ${q.isResolved === "Y" ? "selected" : ""}>해결</option>
              </select>
            </label>
          ` : ``}

          <button class="qna-btn primary" data-act="a-create">등록</button>
          <button class="qna-btn" data-act="a-cancel">닫기</button>
        </div>
        <div class="qna-hint" data-role="a-hint"></div>
      </div>

      <!-- ✅ 질문자 댓글 작성 (원글에만) -->
      <div class="qna-comment-compose" data-role="c-compose" style="display:none;">
        <textarea class="qna-ta" placeholder="질문에 대한 댓글을 입력하세요."></textarea>
        <div class="qna-row">
          <button class="qna-btn primary" data-act="c-create">등록</button>
          <button class="qna-btn" data-act="c-cancel">닫기</button>
        </div>
        <div class="qna-hint" data-role="c-hint"></div>
      </div>
    </div>
  `;

  wrap.innerHTML = `
    <div class="qna-head">
      <div class="qna-meta">
        <span class="qna-nick">${escapeHtml(q.writerNickname || "사용자")}</span>
        <span class="qna-dot">·</span>
        <span class="qna-time">${escapeHtml(q.createdAt || "")}</span>
        ${resolvedBadge}
      </div>
      <div class="qna-actions">
        ${ownerActions}
      </div>
    </div>

    <div class="qna-content" data-role="q-content">${escapeHtml(q.content || "")}</div>

    <div class="qna-edit" data-role="q-edit" style="display:none;">
      <textarea class="qna-ta">${escapeHtml(q.content || "")}</textarea>
      <div class="qna-row">
        <button class="qna-btn primary" data-act="q-save">저장</button>
        <button class="qna-btn" data-act="q-cancel">취소</button>
      </div>
    </div>

    <div class="qna-answer-area">
      ${!hasThread ? composeBlock : ""}

      <div class="qna-answers" data-role="answers"></div>

      ${hasThread ? composeBlock : ""}
    </div>
  `;

  // ✅ 답변/댓글 렌더
  const answersBox = wrap.querySelector('[data-role="answers"]');
  list.forEach((a) => answersBox.appendChild(renderAnswer(q, a)));

  wrap.addEventListener("click", async (e) => {
    const act = e.target?.dataset?.act;
    if (!act) return;

    const qnaId = q.qnaId;

    if (act === "q-edit") {
      wrap.querySelector('[data-role="q-content"]').style.display = "none";
      wrap.querySelector('[data-role="q-edit"]').style.display = "block";
    }

    if (act === "q-cancel") {
      wrap.querySelector('[data-role="q-edit"]').style.display = "none";
      wrap.querySelector('[data-role="q-content"]').style.display = "block";
    }

    if (act === "q-save") {
      const ta = wrap.querySelector('[data-role="q-edit"] textarea');
      const content = (ta.value || "").trim();
      if (!content) return alert("내용을 입력해 주세요.");

      const res = await fetchWithCsrf(`/api/qna/questions/${qnaId}`, {
        method: "PUT",
        body: JSON.stringify({ content }),
      });

      if (res.status === 403) return alert("본인 질문만 수정할 수 있어요.");
      if (!res.ok) return alert("수정 실패");

      await qnaLoad();
    }

    if (act === "q-del") {
      if (!confirm("정말 삭제할까요? (삭제 시 답변/댓글도 함께 숨김 처리됩니다)")) return;

      const courseId = document.getElementById("course-id")?.value;
      if (!courseId) return alert("courseId를 찾을 수 없어요.");

      const res = await fetchWithCsrf(`/api/qna/questions/${qnaId}?courseId=${courseId}`, {
        method: "DELETE",
      });

      if (res.status === 403) return alert("삭제 권한이 없어요.");
      if (!res.ok) return alert("삭제 실패");

      await qnaLoad();
    }

    // ✅ 관리자 답변 작성 UI
    if (act === "a-open") wrap.querySelector('[data-role="a-compose"]').style.display = "block";
    if (act === "a-cancel") wrap.querySelector('[data-role="a-compose"]').style.display = "none";

    if (act === "a-create") {
      const ta = wrap.querySelector('[data-role="a-compose"] textarea');
      const hint = wrap.querySelector('[data-role="a-hint"]');
      const resolvedSel = wrap.querySelector('[data-role="resolved"]');

      const content = (ta.value || "").trim();
      if (!content) {
        if (hint) hint.textContent = "답변 내용을 입력해 주세요.";
        return;
      }
      if (hint) hint.textContent = "";

      const courseId = document.getElementById("course-id")?.value;
      if (!courseId) return alert("courseId를 찾을 수 없어요.");

      const res = await fetchWithCsrf(`/api/qna/answers?courseId=${courseId}`, {
        method: "POST",
        body: JSON.stringify({
          qnaId,
          content,
          parentAnswerId: null,
          isResolved: resolvedSel ? resolvedSel.value : null,
        }),
      });

      if (res.status === 403) {
        if (hint) hint.textContent = "관리자(전체/강의)만 답변할 수 있어요.";
        return;
      }
      if (!res.ok) {
        if (hint) hint.textContent = "답변 등록 실패";
        return;
      }

      ta.value = "";
      wrap.querySelector('[data-role="a-compose"]').style.display = "none";
      await qnaLoad();
    }

    // ✅ 질문자 댓글 작성 UI
    if (act === "c-open") wrap.querySelector('[data-role="c-compose"]').style.display = "block";
    if (act === "c-cancel") wrap.querySelector('[data-role="c-compose"]').style.display = "none";

    if (act === "c-create") {
      const ta = wrap.querySelector('[data-role="c-compose"] textarea');
      const hint = wrap.querySelector('[data-role="c-hint"]');

      const content = (ta.value || "").trim();
      if (!content) {
        if (hint) hint.textContent = "댓글 내용을 입력해 주세요.";
        return;
      }
      if (hint) hint.textContent = "";

      const courseId = document.getElementById("course-id")?.value;
      if (!courseId) return alert("courseId를 찾을 수 없어요.");

      const res = await fetchWithCsrf(`/api/qna/answers?courseId=${courseId}`, {
        method: "POST",
        body: JSON.stringify({
          qnaId,
          content,
          parentAnswerId: null,
          isResolved: null,
        }),
      });

      if (res.status === 403) {
        if (hint) hint.textContent = "질문 작성자만 댓글을 작성할 수 있어요.";
        return;
      }
      if (!res.ok) {
        if (hint) hint.textContent = "댓글 등록 실패";
        return;
      }

      ta.value = "";
      wrap.querySelector('[data-role="c-compose"]').style.display = "none";
      await qnaLoad();
    }
  });

  return wrap;
}

function renderAnswer(question, a) {
  const el = document.createElement("div");
  el.className = a.isComment ? "qna-answer comment" : "qna-answer";
  el.dataset.answerId = a.answerId;

  const editBtn = a.canEdit ? `<button class="qna-link" data-act="a-edit">수정</button>` : ``;
  const delBtn = a.canDelete ? `<button class="qna-link danger" data-act="a-del">삭제</button>` : ``;

  el.innerHTML = `
    <div class="qna-answer-head">
      <div class="qna-meta">
        <span class="qna-nick">${escapeHtml(a.writerNickname || "사용자")}</span>
        <span class="qna-dot">·</span>
        <span class="qna-time">${escapeHtml(a.createdAt || "")}</span>
        ${a.isComment ? `<span class="qna-badge small">댓글</span>` : `<span class="qna-badge small">답변</span>`}
      </div>
      <div class="qna-actions">
        ${editBtn}
        ${delBtn}
      </div>
    </div>

    <div class="qna-content" data-role="a-content">${escapeHtml(a.content || "")}</div>

    <div class="qna-edit" data-role="a-edit" style="display:none;">
      <textarea class="qna-ta">${escapeHtml(a.content || "")}</textarea>
      <div class="qna-row">
        <button class="qna-btn primary" data-act="a-save">저장</button>
        <button class="qna-btn" data-act="a-cancel">취소</button>
      </div>
    </div>
  `;

  el.addEventListener("click", async (e) => {
    const act = e.target?.dataset?.act;
    if (!act) return;

    const answerId = a.answerId;

    if (act === "a-edit") {
      el.querySelector('[data-role="a-content"]').style.display = "none";
      el.querySelector('[data-role="a-edit"]').style.display = "block";
    }

    if (act === "a-cancel") {
      el.querySelector('[data-role="a-edit"]').style.display = "none";
      el.querySelector('[data-role="a-content"]').style.display = "block";
    }

    if (act === "a-save") {
      const ta = el.querySelector('[data-role="a-edit"] textarea');
      const content = (ta.value || "").trim();
      if (!content) return alert("내용을 입력해 주세요.");

      const courseId = document.getElementById("course-id")?.value;
      if (!courseId) return alert("courseId를 찾을 수 없어요.");

      // ✅ 댓글/답변 모두 수정 API 동일
      const res = await fetchWithCsrf(`/api/qna/answers/${answerId}?courseId=${courseId}`, {
        method: "PUT",
        body: JSON.stringify({ content, isResolved: null }),
      });

      if (res.status === 403) return alert("수정 권한이 없어요.");
      if (!res.ok) return alert("수정 실패");

      await qnaLoad();
    }

    if (act === "a-del") {
      if (!confirm("삭제할까요?")) return;

      const courseId = document.getElementById("course-id")?.value;
      if (!courseId) return alert("courseId를 찾을 수 없어요.");

      const res = await fetchWithCsrf(`/api/qna/answers/${answerId}?courseId=${courseId}`, { method: "DELETE" });

      if (res.status === 403) return alert("삭제 권한이 없어요.");
      if (!res.ok) return alert("삭제 실패");

      await qnaLoad();
    }
  });

  return el;
}

function escapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}
