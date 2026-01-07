document.addEventListener("DOMContentLoaded", function () {
    const btnMenu   = document.getElementById("btn-menu");
    const mobileNav = document.getElementById("mobile-nav");
    const overlay   = document.getElementById("mobile-nav-overlay");
    const btnClose  = document.getElementById("mobile-nav-close");

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

    if (btnMenu)  btnMenu.addEventListener("click", openNav);
    if (overlay)  overlay.addEventListener("click", closeNav);
    if (btnClose) btnClose.addEventListener("click", closeNav);

    /* ============================
       모바일 강의목록: 아이콘 클릭 시에만 토글
    ============================ */
    const mobileCourseToggle  = document.getElementById("mobileCourseToggle");
    const mobileCourseSubmenu = document.getElementById("mobileCourseSubmenu");

    if (mobileCourseToggle && mobileCourseSubmenu) {
        mobileCourseToggle.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            mobileCourseSubmenu.classList.toggle("is-open");
            mobileCourseToggle.classList.toggle("is-open");
        });
    }
});
