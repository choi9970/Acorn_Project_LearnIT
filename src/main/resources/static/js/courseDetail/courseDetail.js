document.addEventListener("DOMContentLoaded", () => {
  // ✅ 모달 DOM
  const modalDup = document.getElementById("modal-cart-dup");
  const btnDupYes = document.getElementById("btn-cart-dup-yes");
  const btnDupNo = document.getElementById("btn-cart-dup-no");

  const modalAdded = document.getElementById("modal-cart-added");
  const btnAddedYes = document.getElementById("btn-cart-added-yes");
  const btnAddedNo = document.getElementById("btn-cart-added-no");

  const modalIncomplete = document.getElementById("modal-incomplete");
  const btnIncompleteOk = document.getElementById("btn-incomplete-ok");

  // ✅ 중복 모달: 예 -> 장바구니 이동 / 아니오 -> 닫기
  if (btnDupYes) btnDupYes.addEventListener("click", () => (location.href = "/cart"));
  if (btnDupNo) btnDupNo.addEventListener("click", () => {
    if (modalDup) modalDup.style.display = "none";
  });

  // ✅ 신규 담김 모달: 예 -> 장바구니 이동 / 아니오 -> 닫기
  if (btnAddedYes) btnAddedYes.addEventListener("click", () => (location.href = "/cart"));
  if (btnAddedNo) btnAddedNo.addEventListener("click", () => {
    if (modalAdded) modalAdded.style.display = "none";
  });

  // ✅ 이어보기(미완성) 모달 닫기
  if (btnIncompleteOk) btnIncompleteOk.addEventListener("click", () => {
    if (modalIncomplete) modalIncomplete.style.display = "none";
  });

  // ✅ 버튼 액션 처리
  document.querySelectorAll("[data-action]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const action = btn.dataset.action;

      // 1) 비로그인: 로그인 필요
      if (action === "login") {
        location.href = "/login";
        return;
      }

      // 2) 수강신청
      if (action === "enroll") {
        const courseId = btn.dataset.courseId;
        location.href = "/payment?courseIds=" + courseId;
        return;
      }

      // 3) 장바구니 담기
      if (action === "cartAdd") {
        const courseId = btn.dataset.courseId;

        try {
          const res = await fetch("/cart/add", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({ courseId }),
          });

          if (res.status === 401) {
            location.href = "/login";
            return;
          }

          const text = (await res.text()).trim();
          if (text === "LOGIN_REQUIRED") {
            location.href = "/login";
            return;
          }

          if (text === "DUPLICATE") {
            if (modalDup) modalDup.style.display = "flex";
            return;
          }

          if (text === "OK") {
            // 헤더 뱃지 실시간 갱신
            document.dispatchEvent(new CustomEvent("cart:updated"));
            if (modalAdded) modalAdded.style.display = "flex";
            return;
          }

          alert("알 수 없는 응답: " + text);
        } catch (e) {
          alert("장바구니 담기에 실패했습니다. 서버 상태를 확인하세요.");
        }
        return;
      }

      // 4) 수강중: 이어보기
      if (action === "continue") {
        const courseId = btn.dataset.courseId;
        const chapterId = btn.dataset.chapterId;

        if (courseId && chapterId) {
            location.href = `/course/play?courseId=${courseId}&chapterId=${chapterId}`;
        } else {
            alert("학습 기록이 없거나 챕터 정보를 찾을 수 없습니다.");
        }
        return;
      }
    });
  });
});
