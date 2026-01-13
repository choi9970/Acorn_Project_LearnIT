// /js/admin/admin-qna.js
document.addEventListener("DOMContentLoaded", () => {

  // =========================
  // 0) (선택) URL 파라미터로 특정 Q&A 자동 오픈/하이라이트
  //  - 관리자 페이지에서 "상세로 이동" 시 활용 가능
  //  - 예: /admin/qna?openAdminQna=1&qnaId=101
  // =========================
  const params = new URLSearchParams(location.search);
  const openAdminQna = params.get("openAdminQna"); // ✅ 관리자 페이지에서 쓸 거면 이걸 사용
  const qnaId = params.get("qnaId");

  if (openAdminQna === "1" && qnaId) {
    // main-row에 data-qna-id="101" 같은게 있어야 함 (없으면 아래에서 찾기 로직만 일부 수정)
    const mainRow =
      document.querySelector(`tr.main-row[data-qna-id="${qnaId}"]`) ||
      document.querySelector(`tr.main-row[data-id="${qnaId}"]`);

    if (mainRow) {
      const detail = mainRow.nextElementSibling;
      if (detail && detail.classList.contains("detail-row")) {
        detail.style.display = "table-row";
        mainRow.scrollIntoView({ behavior: "smooth", block: "center" });
        mainRow.classList.add("highlight-qna");
        setTimeout(() => mainRow.classList.remove("highlight-qna"), 1500);
      }
    }
  }

  // =========================
  // 1) 행 클릭 → 아코디언 토글
  // =========================
  document.querySelectorAll("tr.main-row").forEach((row) => {
    row.addEventListener("click", (e) => {
      const tag = (e.target.tagName || "").toLowerCase();
      // 클릭 예외(버튼/링크/폼 요소)
      if (["button", "a", "select", "textarea", "input", "label", "option", "form"].includes(tag)) return;

      const detail = row.nextElementSibling;
      if (!detail || !detail.classList.contains("detail-row")) return;

      detail.style.display = detail.style.display === "table-row" ? "none" : "table-row";
    });
  });

  // =========================
  // 2) 구분 필터 팝업
  // =========================
  document.querySelectorAll(".filter-icon").forEach((icon) => {
    icon.addEventListener("click", (e) => {
      e.stopPropagation();

      const key = icon.dataset.key;
      const popup = document.querySelector(`.filter-popup[data-popup="${key}"]`);
      if (!popup) return;

      document.querySelectorAll(".filter-popup").forEach((p) => {
        if (p !== popup) p.style.display = "none";
      });

      popup.style.display = popup.style.display === "block" ? "none" : "block";
    });
  });

  // =========================
  // 3) 상태 배지 클릭 → 상태 변경 팝업
  // =========================
  document.querySelectorAll(".status-badge-wrap").forEach((wrap) => {
    const badge = wrap.querySelector(".status-badge-click");
    const popup = wrap.querySelector(".status-action-popup");
    if (!badge || !popup) return;

    badge.addEventListener("click", (e) => {
      e.stopPropagation();

      document.querySelectorAll(".status-action-popup").forEach((p) => {
        if (p !== popup) p.style.display = "none";
      });

      popup.style.display = popup.style.display === "block" ? "none" : "block";
    });

    popup.addEventListener("click", (e) => e.stopPropagation());
  });

  // =========================
  // 4) 바깥 클릭 시 닫기
  // =========================
  document.addEventListener("click", () => {
    document.querySelectorAll(".filter-popup").forEach((p) => (p.style.display = "none"));
    document.querySelectorAll(".status-action-popup").forEach((p) => (p.style.display = "none"));
  });
});
