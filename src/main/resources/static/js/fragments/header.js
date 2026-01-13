document.addEventListener("DOMContentLoaded", function () {
  const btnMenu = document.getElementById("btn-menu");
  const mobileNav = document.getElementById("mobile-nav");
  const overlay = document.getElementById("mobile-nav-overlay");
  const btnClose = document.getElementById("mobile-nav-close");

  // -----------------------------
  // Cart badge: 실시간 갱신
  // -----------------------------
  const badgeEls = () => Array.from(document.querySelectorAll(".cart-count-badge"));

  function setBadgeCount(count) {
    const n = Number.isFinite(count) ? count : 0;
    badgeEls().forEach((el) => {
      el.textContent = String(n);
      el.classList.toggle("is-hidden", n <= 0);
    });
  }

  async function refreshCartBadge() {
    try {
      const res = await fetch("/cart/count", {
        method: "GET",
        headers: { Accept: "text/plain, application/json" },
        cache: "no-store",
      });

      if (!res.ok) return;
      const text = (await res.text()).trim();
      const count = parseInt(text, 10);
      if (!Number.isNaN(count)) setBadgeCount(count);
    } catch (e) {
      // 실패해도 UI는 유지
    }
  }

  // -----------------------------
  // ✅ 로그인 직후 UX 정리(세션도 정리해서 깔끔하게)
  // - (세션) 게스트 장바구니에서 "이미 수강중" 강의 제거
  // - (DB) 회원 장바구니에서도 "이미 수강중" 강의 제거
  // - 최종 count로 뱃지 즉시 동기화
  // -----------------------------
  async function cleanupEnrolledCartUX() {
    try {
      const res = await fetch("/cart/cleanup-enrolled", {
        method: "POST",
        headers: { Accept: "application/json" },
        cache: "no-store",
      });

      if (!res.ok) return;

      const data = await res.json();

      const count = Number(data?.count);
      if (!Number.isNaN(count)) setBadgeCount(count);

      const removedGuest = Number(data?.removedGuest || 0);
      const removedUser = Number(data?.removedUser || 0);
      const removedTotal = removedGuest + removedUser;

      // 제거가 실제로 발생했고, 현재 장바구니 페이지라면 화면도 즉시 반영
      if (removedTotal > 0 && window.location && window.location.pathname === "/cart") {
        window.location.reload();
      }

      // 다른 화면에서도 필요하면 이벤트로 동기화
      if (removedTotal > 0) {
        document.dispatchEvent(new CustomEvent("cart:updated"));
      }
    } catch (e) {
      // 실패해도 전체 기능은 유지
    }
  }

  function openNav() {
    if (!mobileNav) return;
    mobileNav.classList.add("is-open");
    mobileNav.setAttribute("aria-hidden", "false");
  }

  function closeNav() {
    if (!mobileNav) return;
    mobileNav.classList.remove("is-open");
    mobileNav.setAttribute("aria-hidden", "true");
  }

  if (btnMenu) btnMenu.addEventListener("click", openNav);
  if (overlay) overlay.addEventListener("click", closeNav);
  if (btnClose) btnClose.addEventListener("click", closeNav);

  // 최초 1회 뱃지 동기화
  refreshCartBadge();

  // ✅ 로그인 직후(첫 화면) UX 정리 1회 실행
  cleanupEnrolledCartUX();

  // 페이지 내에서 장바구니가 변경되면 이벤트로 갱신
  document.addEventListener("cart:updated", refreshCartBadge);

  // 탭 복귀 시 동기화
  window.addEventListener("focus", refreshCartBadge);

  // 폴링
  setInterval(refreshCartBadge, 5000);
});
