document.addEventListener("DOMContentLoaded", () => {
  const box = document.getElementById("githubBox");
  if (!box) return;

  const user = box.getAttribute("data-github-user");
  if (!user) return;

  const ghLink = document.getElementById("ghLink");
  if (ghLink) ghLink.href = `https://github.com/${user}`;

  const el = (id) => document.getElementById(id);
  const showError = (msg) => {
    const err = el("ghError");
    if (!err) return;
    err.style.display = "block";
    err.textContent = msg;
  };

  const fmtDate = (iso) => {
    if (!iso) return "-";
    const d = new Date(iso);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  };

  (async () => {
    try {
      // 1) 유저(또는 org) 정보
      const uRes = await fetch(`https://api.github.com/users/${encodeURIComponent(user)}`, {
        headers: { "Accept": "application/vnd.github+json" }
      });

      if (!uRes.ok) {
        showError(`GitHub 정보를 불러오지 못했습니다. (${uRes.status})`);
        return;
      }

      const u = await uRes.json();

      el("ghSub").textContent = u.name ? `${u.name} (@${u.login})` : `@${u.login}`;
      el("ghRepos").textContent = u.public_repos ?? "-";
      el("ghFollowers").textContent = u.followers ?? "-";
      el("ghFollowing").textContent = u.following ?? "-";

      // 2) Repo 목록(최근 업데이트)
      const rRes = await fetch(`https://api.github.com/users/${encodeURIComponent(user)}/repos?per_page=100&sort=updated`, {
        headers: { "Accept": "application/vnd.github+json" }
      });

      if (!rRes.ok) {
        showError(`Repo 정보를 불러오지 못했습니다. (${rRes.status})`);
        return;
      }

      const repos = await rRes.json();
      const top = Array.isArray(repos) ? repos.slice(0, 5) : [];

      // last updated = 가장 최신 repo updated_at
      if (top.length > 0) el("ghUpdated").textContent = fmtDate(top[0].updated_at);

      const list = el("repoList");
      if (!list) return;
      list.innerHTML = "";

      if (top.length === 0) {
        list.innerHTML = `<div class="hint">공개 저장소가 없거나 표시할 Repo가 없습니다.</div>`;
        return;
      }

      top.forEach((repo) => {
        const stars = repo.stargazers_count ?? 0;
        const lang = repo.language ?? "N/A";
        const updated = fmtDate(repo.updated_at);

        const div = document.createElement("div");
        div.className = "repo";
        div.innerHTML = `
          <div>
            <a href="${repo.html_url}" target="_blank" rel="noopener">${repo.name}</a>
          </div>
          <div class="repo-meta">
            <span class="badge">★ ${stars}</span>
            <span class="badge">${lang}</span>
            <span>Updated: ${updated}</span>
          </div>
        `;
        list.appendChild(div);
      });
    } catch (e) {
      showError("네트워크 오류로 GitHub 정보를 불러오지 못했습니다.");
    }
  })();
});
