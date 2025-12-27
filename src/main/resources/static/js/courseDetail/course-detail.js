document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-action]").forEach(btn => {
    btn.addEventListener("click", async () => {
      const action = btn.dataset.action;

      if (action === "login") {
        alert("로그인이 필요합니다. (로그인 페이지 구현 후 이동)");
        return;
      }

      // ✅ 수강신청 가기 (원하는 URL로 바꿔)
      if (action === "enroll") {
        const courseId = btn.dataset.courseId;
        // 예시: 수강신청 페이지가 없으면 일단 alert
        alert("수강신청 페이지로 이동(추후 연결) courseId=" + courseId);
        // location.href = `/enroll?courseId=${courseId}`;
        return;
      }

      // ✅ 장바구니 담기: 무조건 먼저 담고 → confirm은 이동만 결정
      if (action === "cartAdd") {
        const courseId = btn.dataset.courseId;

        try {
          const res = await fetch("/cart/add", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({ courseId })
          });

          const text = await res.text(); // OK or DUP
          // text === "DUP"이면 이미 담겨있음 (그래도 유지)

          const ok = confirm("장바구니 페이지로 이동하시겠습니까?");
          if (ok) location.href = "/cart";
          // 취소면 그냥 현재 페이지 유지
        } catch (e) {
          alert("장바구니 담기에 실패했습니다. 서버 상태를 확인하세요.");
        }
        return;
      }
    });
  });
});
