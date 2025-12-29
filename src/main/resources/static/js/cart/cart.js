document.addEventListener("DOMContentLoaded", () => {

    const checkAll = document.getElementById("checkAll");
    const itemChecks = () => Array.from(document.querySelectorAll(".itemCheck"));

    const totalText = document.getElementById("totalPriceText");
    const discountText = document.getElementById("discountText");
    const finalText = document.getElementById("finalPriceText");

    function formatWon(n) {
        return n.toLocaleString("ko-KR") + "원";
    }

    function calcSelectedTotal() {
        return itemChecks()
            .filter(chk => chk.checked)
            .reduce((sum, chk) => sum + Number(chk.dataset.price || 0), 0);
    }

    function renderPrice() {
        const total = calcSelectedTotal();
        const discount = 0;
        const final = total - discount;

        if (totalText) totalText.textContent = formatWon(total);
        if (discountText) discountText.textContent = "-" + formatWon(discount);
        if (finalText) finalText.textContent = formatWon(final);

        const all = itemChecks();
        if (checkAll && all.length > 0) {
            checkAll.checked = all.every(chk => chk.checked);
        }
    }

    if (checkAll) {
        checkAll.addEventListener("change", () => {
            itemChecks().forEach(chk => (chk.checked = checkAll.checked));
            renderPrice();
        });
    }

    itemChecks().forEach(chk => chk.addEventListener("change", renderPrice));

    renderPrice();

    // 결제 버튼
    const summaryBtn = document.querySelector(".summary-btn");
    if (summaryBtn) {
        summaryBtn.addEventListener("click", () => {
            const selected = itemChecks().filter(chk => chk.checked);

            if (selected.length === 0) {
                alert("결제할 강의를 선택하세요.");
                return;
            }

            const courseIds = selected.map(chk =>
                chk.closest(".cart-item")
                    .querySelector("input[name='courseId']").value
            );

            // 결제 페이지로 이동
            location.href = "/payment?courseIds=" + courseIds.join(",");
        });
    }
});

async function addToCart(courseId) {
  const res = await fetch('/cart/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ courseId })
  });

  const text = await res.text();

  if (text === 'LOGIN_REQUIRED') {
    alert('로그인이 필요합니다.');
    location.href = '/login';
    return;
  }
  if (text === 'DUPLICATE') {
    alert('이미 장바구니에 담긴 강의입니다.');
    return;
  }
  if (text === 'OK') {
    alert('장바구니에 담았습니다!');
    return;
  }

  console.log(text);
  alert('처리 중 오류가 발생했습니다.');
}
