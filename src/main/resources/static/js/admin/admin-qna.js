// /js/admin/admin-qna.js
document.addEventListener("DOMContentLoaded", () => {
  // 1) 행 클릭 → 아코디언 토글
  document.querySelectorAll("tr.main-row").forEach((row) => {
    row.addEventListener("click", (e) => {
      const tag = (e.target.tagName || "").toLowerCase();
      if (["button", "a", "select", "textarea", "input", "label", "option", "form"].includes(tag)) return;

      const detail = row.nextElementSibling;
      if (!detail || !detail.classList.contains("detail-row")) return;

      detail.style.display = detail.style.display === "table-row" ? "none" : "table-row";
    });
  });

  // 2) 구분 필터 팝업
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

  // 3) 상태 배지 클릭 → 상태 변경 팝업
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

  // 4) 바깥 클릭시 닫기
  document.addEventListener("click", () => {
    document.querySelectorAll(".filter-popup").forEach((p) => (p.style.display = "none"));
    document.querySelectorAll(".status-action-popup").forEach((p) => (p.style.display = "none"));
  });
});
