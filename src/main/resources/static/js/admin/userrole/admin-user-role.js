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
  const res = await fetch(url, {
    credentials: "same-origin",
    ...options,
    headers
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || `HTTP ${res.status}`);
  }

  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) return await res.json();
  return null;
}

async function loadUsers(page = currentPage) {
  currentPage = page;

  const type = document.getElementById("searchType").value;
  const keyword = document.getElementById("searchKeyword").value.trim();

  const data = await apiFetch(`/api/admin/users?type=${encodeURIComponent(type)}&keyword=${encodeURIComponent(keyword)}&page=${page}&size=7`, {
    method: "GET"
  });

  renderUsers(data.items || []);
  renderPagination(data.page, data.totalPages);
}

function renderUsers(users) {
  const tbody = document.getElementById("userTbody");
  tbody.innerHTML = "";

  users.forEach(u => {
    const tr = document.createElement("tr");

    const managed = (u.managedCourses || [])
      .map(c => `<span class="tag" data-id="${c.courseId}">${escapeHtml(c.title)}</span>`)
      .join(" ");

    tr.innerHTML = `
      <td>${u.userId}</td>
      <td>${escapeHtml(u.name || "")}</td>
      <td>${escapeHtml(u.email || "")}</td>

      <td>
        <div class="cell-line">
          <select class="status">
            ${["SIGNUP_PENDING","ACTIVE","BANNED","DELETE"].map(s =>
              `<option value="${s}" ${u.status===s?"selected":""}>${s}</option>`
            ).join("")}
          </select>
          <button class="btn-status" type="button">저장</button>
        </div>

        <div class="pending-box" style="display:none;">
          <input class="nickname" placeholder="닉네임" />
          <input class="phone" placeholder="010-0000-0000" />
          <div class="hint">소셜 SIGNUP_PENDING → ACTIVE는 닉네임/전화번호가 필요합니다.</div>
        </div>
      </td>

      <td>
        <div class="cell-line">
          <select class="role">
            ${["USER","SUB_ADMIN","ADMIN"].map(r =>
              `<option value="${r}" ${u.role===r?"selected":""}>${r}</option>`
            ).join("")}
          </select>
          <button class="btn-role" type="button">저장</button>
        </div>

        <div class="subadmin-box" style="display:${u.role==="SUB_ADMIN" ? "block" : "none"};">
          <div class="sub-line">
            <input class="course-keyword" placeholder="강의 검색" />
            <button class="btn-course-search" type="button">검색</button>
          </div>
          <div class="sub-line">
            <select class="course-select"></select>
            <button class="btn-course-add" type="button">+</button>
          </div>
          <div class="managed">${managed}</div>
        </div>
      </td>
    `;

    tbody.appendChild(tr);

    const statusSel = tr.querySelector(".status");
    const pendingBox = tr.querySelector(".pending-box");
    const roleSel = tr.querySelector(".role");
    const subBox = tr.querySelector(".subadmin-box");

    statusSel.addEventListener("change", () => {
      const isSocial = u.provider && u.provider.toLowerCase() !== "local";
      if (u.status === "SIGNUP_PENDING" && statusSel.value === "ACTIVE" && isSocial) {
        pendingBox.style.display = "block";
      } else {
        pendingBox.style.display = "none";
      }
    });

    roleSel.addEventListener("change", () => {
      subBox.style.display = (roleSel.value === "SUB_ADMIN") ? "block" : "none";
    });

    tr.querySelector(".btn-status").addEventListener("click", () => saveStatus(u, tr));
    tr.querySelector(".btn-role").addEventListener("click", () => saveRole(u, tr));
    tr.querySelector(".btn-course-search")?.addEventListener("click", () => searchCourse(tr));
    tr.querySelector(".btn-course-add")?.addEventListener("click", () => addCourse(tr));
  });
}

async function saveStatus(u, tr) {
  const next = tr.querySelector(".status").value;
  const isSocial = u.provider && u.provider.toLowerCase() !== "local";

  let nickname = null;
  let phone = null;

  if (u.status === "SIGNUP_PENDING" && next === "ACTIVE" && isSocial) {
    nickname = tr.querySelector(".nickname").value.trim();
    phone = tr.querySelector(".phone").value.trim();
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
    loadUsers();
  } catch (e) {
    alert(e.message || "상태 변경 실패");
    console.log(e.message);
  }
}

async function saveRole(u, tr) {
  const role = tr.querySelector(".role").value;
  const isSocial = u.provider && u.provider.toLowerCase() !== "local";

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
    loadUsers();
  } catch (e) {
    alert(e.message || "권한 변경 실패");
  }
}

async function searchCourse(tr) {
  const kw = tr.querySelector(".course-keyword").value.trim();
  try {
    const data = await apiFetch(`/api/admin/courses?keyword=${encodeURIComponent(kw)}&page=1&size=7`, {
      method: "GET"
    });
    const sel = tr.querySelector(".course-select");
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
  if (!sel.value) return;

  const list = tr.querySelector(".managed");
  const exists = [...list.querySelectorAll(".tag")].some(t => t.dataset.id === String(sel.value));
  if (exists) return;

  const tag = document.createElement("span");
  tag.className = "tag";
  tag.dataset.id = sel.value;
  tag.textContent = sel.selectedOptions[0]?.textContent || sel.value;
  list.appendChild(tag);
}

function renderPagination(page, totalPages) {
  const el = document.getElementById("pagination");
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

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
