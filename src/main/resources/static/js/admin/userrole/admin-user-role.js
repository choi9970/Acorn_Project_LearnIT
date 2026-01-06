let currentPage = 1;

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("btnSearch")?.addEventListener("click", () => {
    currentPage = 1;
    loadUsers();
  });
  loadUsers();
});

function csrfHeaders() {
  const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
  const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
  if (token && header) return { [header]: token };
  return {};
}

async function apiFetch(url, options = {}) {
  const headers = { ...(options.headers || {}), ...csrfHeaders() };
  const res = await fetch(url, { credentials: "same-origin", ...options, headers });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || `HTTP ${res.status}`);
  }

  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) return await res.json();
  return null;
}

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

// ✅ 서버 전이 규칙과 동일하게(현재 상태 유지 포함)
function allowedNextStatuses(current) {
  if (current === "SIGNUP_PENDING") return ["SIGNUP_PENDING", "ACTIVE"];
  if (current === "ACTIVE") return ["ACTIVE", "BANNED", "DELETE"]; // SIGNUP_PENDING 불가
  if (current === "BANNED") return ["BANNED", "ACTIVE"];
  if (current === "DELETE") return ["DELETE", "ACTIVE"];
  return ["SIGNUP_PENDING", "ACTIVE", "BANNED", "DELETE"];
}

// ✅ 전이 불가 옵션은 disabled가 아니라 "옵션 자체를 제거"해서 아예 안 보이게 함
function applyStatusTransitionRules(statusSelectEl, curr) {
  if (!statusSelectEl) return;

  const allowed = allowedNextStatuses(curr);

  // ✅ 원본 option(text) 백업 (처음 1회)
  if (!statusSelectEl.dataset.allOptionsJson) {
    const all = Array.from(statusSelectEl.options).map(o => ({
      value: o.value,
      text: o.textContent
    }));
    statusSelectEl.dataset.allOptionsJson = JSON.stringify(all);
  }

  const allOptions = JSON.parse(statusSelectEl.dataset.allOptionsJson);

  // ✅ 허용된 옵션만 다시 렌더링 (불가 옵션은 "목록에서 사라짐")
  statusSelectEl.innerHTML = allowed
    .map(v => {
      const found = allOptions.find(x => x.value === v);
      const text = found ? found.text : v;
      return `<option value="${v}">${text}</option>`;
    })
    .join("");

  // ✅ 기본 선택값은 항상 현재 상태
  statusSelectEl.value = curr;
}

function setStatusEditMode(tr, on) {
  tr.classList.toggle("status-editing", !!on);

  const sel = tr.querySelector(".status");
  const btn = tr.querySelector(".btn-status");
  const pendingBox = tr.querySelector(".pending-box");
  const nickname = tr.querySelector(".nickname");
  const phone = tr.querySelector(".phone");

  if (sel) sel.disabled = !on;
  if (nickname) nickname.disabled = !on;
  if (phone) phone.disabled = !on;

  if (!on) {
    if (pendingBox) pendingBox.style.display = "none";
    if (btn) btn.textContent = "수정";
  } else {
    if (btn) btn.textContent = "저장";
  }
}

function setRoleEditMode(tr, on) {
  tr.classList.toggle("role-editing", !!on);

  const roleSel = tr.querySelector(".role");
  const btn = tr.querySelector(".btn-role");

  const kw = tr.querySelector(".course-keyword");
  const courseSel = tr.querySelector(".course-select");
  const btnCourseSearch = tr.querySelector(".btn-course-search");
  const btnCourseAdd = tr.querySelector(".btn-course-add");

  if (roleSel) roleSel.disabled = !on;
  if (kw) kw.disabled = !on;
  if (courseSel) courseSel.disabled = !on;
  if (btnCourseSearch) btnCourseSearch.disabled = !on;
  if (btnCourseAdd) btnCourseAdd.disabled = !on;

  if (btn) btn.textContent = on ? "저장" : "수정";
}

function buildManagedTag(courseId, title) {
  return `
    <span class="tag" data-id="${courseId}">
      <span class="tag-text">${escapeHtml(title)}</span>
      <button type="button" class="tag-del" title="삭제" aria-label="삭제">×</button>
    </span>
  `;
}

async function loadUsers(page = currentPage) {
  currentPage = page;

  const type = document.getElementById("searchType")?.value || "email";
  const keyword = document.getElementById("searchKeyword")?.value?.trim() || "";

  const data = await apiFetch(
    `/api/admin/users?type=${encodeURIComponent(type)}&keyword=${encodeURIComponent(keyword)}&page=${page}&size=7`,
    { method: "GET" }
  );

  renderUsers(data.items || []);
  renderPagination(data.page, data.totalPages);
}

function renderUsers(users) {
  const tbody = document.getElementById("userTbody");
  if (!tbody) return;
  tbody.innerHTML = "";

  users.forEach(u => {
    const tr = document.createElement("tr");

    const managedHtml = (u.managedCourses || [])
      .map(c => buildManagedTag(c.courseId, c.title))
      .join(" ");

    tr.innerHTML = `
      <td>${u.userId}</td>
      <td>${escapeHtml(u.name || "")}</td>
      <td>${escapeHtml(u.email || "")}</td>

      <td>
        <div class="cell-line">
          <select class="status" disabled>
            ${["SIGNUP_PENDING","ACTIVE","BANNED","DELETE"].map(s =>
              `<option value="${s}" ${u.status===s?"selected":""}>${s}</option>`
            ).join("")}
          </select>
          <button class="btn-status" type="button">수정</button>
        </div>

        <div class="pending-box" style="display:none;">
          <input class="nickname" placeholder="닉네임" disabled />
          <input class="phone" placeholder="010-0000-0000" disabled />
          <div class="hint">소셜 SIGNUP_PENDING → ACTIVE는 닉네임/전화번호가 필요합니다.</div>
        </div>
      </td>

      <td>
        <div class="cell-line">
          <select class="role" disabled>
            ${["USER","SUB_ADMIN","ADMIN"].map(r =>
              `<option value="${r}" ${u.role===r?"selected":""}>${r}</option>`
            ).join("")}
          </select>
          <button class="btn-role" type="button">수정</button>
        </div>

        <div class="subadmin-box" style="display:${u.role==="SUB_ADMIN" ? "block" : "none"};">
          <div class="sub-line">
            <input class="course-keyword" placeholder="강의 검색" disabled />
            <button class="btn-course-search" type="button" disabled>검색</button>
          </div>
          <div class="sub-line">
            <select class="course-select" disabled></select>
            <button class="btn-course-add" type="button" disabled>+</button>
          </div>
          <div class="managed">${managedHtml}</div>
        </div>
      </td>
    `;

    tbody.appendChild(tr);

    // ✅ 전이 불가 옵션은 아예 안 보이게(옵션 제거)
    const statusSel = tr.querySelector(".status");
    applyStatusTransitionRules(statusSel, u.status);

    const pendingBox = tr.querySelector(".pending-box");
    const roleSel = tr.querySelector(".role");
    const subBox = tr.querySelector(".subadmin-box");

    roleSel?.addEventListener("change", () => {
      if (subBox) subBox.style.display = (roleSel.value === "SUB_ADMIN") ? "block" : "none";
    });

    // ✅ change에서 전이룰 재적용하면 선택값이 초기화되므로 여기서는 pending box만 제어
    statusSel?.addEventListener("change", () => {
      const isSocial = u.provider && String(u.provider).toLowerCase() !== "local";
      if (u.status === "SIGNUP_PENDING" && statusSel.value === "ACTIVE" && isSocial) {
        if (pendingBox) pendingBox.style.display = "block";
      } else {
        if (pendingBox) pendingBox.style.display = "none";
      }
    });

    // 기본은 보기모드(전부 비활성화)
    setStatusEditMode(tr, false);
    setRoleEditMode(tr, false);

    // ✅ 상태: 수정 -> 저장 토글
    tr.querySelector(".btn-status")?.addEventListener("click", async () => {
      const editing = tr.classList.contains("status-editing");
      if (!editing) {
        setStatusEditMode(tr, true);
        return;
      }
      await saveStatus(u, tr);
    });

    // ✅ 권한: 수정 -> 저장 토글
    tr.querySelector(".btn-role")?.addEventListener("click", async () => {
      const editing = tr.classList.contains("role-editing");
      if (!editing) {
        setRoleEditMode(tr, true);
        return;
      }
      await saveRole(u, tr);
    });

    tr.querySelector(".btn-course-search")?.addEventListener("click", () => {
      if (!tr.classList.contains("role-editing")) return;
      searchCourse(tr);
    });

    tr.querySelector(".btn-course-add")?.addEventListener("click", () => {
      if (!tr.classList.contains("role-editing")) return;
      addCourse(tr);
    });

    // ✅ SUB_ADMIN 태그 “삭제(×)” - 수정모드에서만 + 즉시 서버 반영
    tr.querySelector(".managed")?.addEventListener("click", (ev) => {
      const btn = ev.target.closest?.(".tag-del");
      if (!btn) return;
      if (!tr.classList.contains("role-editing")) return;

      const tag = btn.closest(".tag");
      const courseId = Number(tag?.dataset?.id);
      if (!courseId) return;

      const tags = tr.querySelectorAll(".managed .tag");
      if (tags.length <= 1) {
        alert("SUB_ADMIN은 최소 1개 이상의 관리 강의가 필요합니다.");
        return;
      }

      deleteManagedCourse(u.userId, courseId, tag);
    });
  });
}

async function saveStatus(u, tr) {
  const next = tr.querySelector(".status")?.value;
  if (!next || next === u.status) {
    setStatusEditMode(tr, false);
    return;
  }
  const isSocial = u.provider && String(u.provider).toLowerCase() !== "local";

  let nickname = null;
  let phone = null;

  if (u.status === "SIGNUP_PENDING" && next === "ACTIVE" && isSocial) {
    nickname = tr.querySelector(".nickname")?.value?.trim() || "";
    phone = tr.querySelector(".phone")?.value?.trim() || "";
    if (!nickname || !phone) {
      alert("닉네임/전화번호가 필요합니다.");
      return;
    }
  }

  try {
    await apiFetch(`/api/admin/users/${u.userId}/status`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: next, nickname, phone })
    });
    alert("상태 변경 완료");
    setStatusEditMode(tr, false);
    loadUsers();
  } catch (e) {
    alert(e.message || "상태 변경 실패");
  }
}

async function saveRole(u, tr) {
  const role = tr.querySelector(".role")?.value;
  const isSocial = u.provider && String(u.provider).toLowerCase() !== "local";

  if (isSocial && (role === "ADMIN" || role === "SUB_ADMIN")) {
    alert("소셜 가입 회원에게 ADMIN/SUB_ADMIN 권한을 부여할 수 없습니다.");
    return;
  }

  let courseIds = [];
  if (role === "SUB_ADMIN") {
    courseIds = [...tr.querySelectorAll(".managed .tag")]
      .map(s => Number(s.dataset.id))
      .filter(Boolean);

    if (courseIds.length < 1) {
      alert("SUB_ADMIN은 1개 이상의 관리 강의가 필요합니다.");
      return;
    }
  }

  try {
    await apiFetch(`/api/admin/users/${u.userId}/role`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ role, courseIds })
    });
    alert("권한 변경 완료");
    setRoleEditMode(tr, false);
    loadUsers();
  } catch (e) {
    alert(e.message || "권한 변경 실패");
  }
}

async function searchCourse(tr) {
  const kw = tr.querySelector(".course-keyword")?.value?.trim() || "";
  try {
    const data = await apiFetch(`/api/admin/courses?keyword=${encodeURIComponent(kw)}&page=1&size=7`, {
      method: "GET"
    });
    const sel = tr.querySelector(".course-select");
    if (!sel) return;

    sel.innerHTML = "";
    (data.items || []).forEach(c => {
      const opt = document.createElement("option");
      opt.value = c.courseId;
      opt.textContent = c.title;
      sel.appendChild(opt);
    });
  } catch (e) {
    alert(e.message || "강의 검색 실패");
  }
}

function addCourse(tr) {
  const sel = tr.querySelector(".course-select");
  if (!sel || !sel.value) return;

  const list = tr.querySelector(".managed");
  if (!list) return;

  const exists = [...list.querySelectorAll(".tag")].some(t => t.dataset.id === String(sel.value));
  if (exists) return;

  const title = sel.selectedOptions[0]?.textContent || sel.value;
  list.insertAdjacentHTML("beforeend", buildManagedTag(sel.value, title));
}

async function deleteManagedCourse(userId, courseId, tagEl) {
  try {
    await apiFetch(`/api/admin/users/${userId}/sub-admin/courses/${courseId}`, { method: "DELETE" });
    tagEl?.remove();
  } catch (e) {
    alert(e.message || "삭제 실패");
  }
}

function renderPagination(page, totalPages) {
  const el = document.getElementById("pagination");
  if (!el) return;

  el.innerHTML = "";
  if (!totalPages || totalPages <= 1) return;

  for (let p = 1; p <= totalPages; p++) {
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = p;
    b.disabled = (p === page);
    b.addEventListener("click", () => loadUsers(p));
    el.appendChild(b);
  }
}
