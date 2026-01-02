document.addEventListener("DOMContentLoaded", function () {
    // 1. HTML의 data 속성에서 데이터 가져오기
    const container = document.getElementById("paymentContainer");

    const courseIds = JSON.parse(container.dataset.courseIds);
    const basePrice = Number(container.dataset.basePrice);

    let selectedCouponId = null;
    let discountAmount = 0;

    const totalText = document.getElementById("totalPriceText");
    const discountText = document.getElementById("discountText");
    const finalText = document.getElementById("finalPriceText");
    const kakaoBtn = document.getElementById("kakaoPayBtn");
    const cardBtn = document.getElementById("cardPayBtn");
    const couponSelect = document.getElementById("couponSelect");
    const warningText = document.getElementById("couponWarning");

    warningText.style.display = "none";
    couponSelect.classList.remove("invalid");

    // 금액 포맷 함수
    function formatWon(n) {
        return n.toLocaleString("ko-KR") + "원";
    }

    // 화면 갱신
    function renderPrice() {
        let finalPrice = basePrice - discountAmount;
        if (finalPrice < 0) finalPrice = 0;

        totalText.textContent = formatWon(basePrice);
        discountText.textContent = "-" + formatWon(discountAmount);
        finalText.textContent = formatWon(finalPrice);
    }

    // 로딩 상태 제어
    function setBtnLoading(btn, loading) {
        btn.disabled = loading;
        btn.setAttribute("aria-busy", String(loading));
    }

    // 쿠폰 선택 이벤트
    couponSelect?.addEventListener("change", (e) => {
        const opt = e.target.selectedOptions[0];
        const minPrice = opt?.dataset.minPrice ? Number(opt.dataset.minPrice) : 0;
        discountAmount = opt?.dataset.discount ? Number(opt.dataset.discount) : 0;
        selectedCouponId = opt?.value || null;

        if (!selectedCouponId) {
            discountAmount = 0;
            selectedCouponId = null;
            warningText.style.display = "none";
            couponSelect.classList.remove("invalid");
            kakaoBtn.disabled = false;
            cardBtn.disabled = false;

            renderPrice();
            return;
        }

        //쿠폰 실시간 검증
        if (basePrice < minPrice) {
            discountAmount = 0;
            selectedCouponId = opt.value;

            warningText.textContent =
                `이 쿠폰은 ${minPrice.toLocaleString()}원 이상 결제 시 사용 가능합니다.`;
            warningText.style.display = "flex";
            couponSelect.classList.add("invalid");

            kakaoBtn.disabled = true;
            cardBtn.disabled = true;

            renderPrice();
            return;
        }

        // 쿠폰 선택 + 조건 충족
        discountAmount = Number(opt.dataset.discount);
        selectedCouponId = opt.value;

        warningText.style.display = "none";
        couponSelect.classList.remove("invalid");

        kakaoBtn.disabled = false;
        cardBtn.disabled = false;

        renderPrice();
    });

    //응답 형태 처리
    async function checkResponse(res) {
        if (res.status === 401) {
            const data = await res.json();
            alert(data.message || "로그인이 필요한 서비스입니다.");
            location.href = "/login";
            throw new Error("UNAUTHORIZED");
        }
        if (!res.ok) {
            const errorText = await res.text();
            let errorMessage = "오류가 발생했습니다.";

            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = errorJson.message || errorMessage;
            } catch(e) {
                errorMessage = errorText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        return res.json();
    }

    // 카카오페이 결제
    kakaoBtn?.addEventListener("click", () => {
        setBtnLoading(kakaoBtn, true);

        fetch("/payments/kakao/ready", {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                courseIds: courseIds,
                totalPrice: basePrice - discountAmount,
                couponId: selectedCouponId
            })
        })
            .then(checkResponse)
            .then(data => location.href = data.next_redirect_pc_url)
            .catch(err => {
                if(err.message !== "UNAUTHORIZED") alert(err.message);
                setBtnLoading(kakaoBtn, false);
            });
    });

    // 일반 카드 결제
    cardBtn?.addEventListener("click", () => {
        setBtnLoading(cardBtn, true);

        fetch("/payments/card/ready", {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                courseIds: courseIds,
                couponId: selectedCouponId
            })
        })
            .then(checkResponse)
            .then(data => {
                // PortOne 초기화 (가맹점 식별코드)
                IMP.init("imp45313105");

                IMP.request_pay({
                    pg: "html5_inicis",
                    pay_method: "card",
                    merchant_uid: data.orderNo,
                    name: "강의 결제 테스트",
                    amount: data.amount
                }, function (rsp) {
                    if (!rsp.success) {
                        alert("결제 실패: " + rsp.error_msg);
                        setBtnLoading(cardBtn, false);
                        return;
                    }

                    // 서버에 결제 완료 통지
                    fetch("/payments/card/complete", {
                        method: "POST",
                        credentials: "include",
                        headers: {
                            "Content-Type": "application/json",
                            "Accept": "application/json"
                        },
                        body: JSON.stringify({
                            impUid: rsp.imp_uid,
                            merchantUid: rsp.merchant_uid
                        })
                    })
                        .then(checkResponse)
                        .then(result => {
                            location.href = "/payment/result?orderNo=" + encodeURIComponent(result.orderNo);
                        })
                        .catch(err => {
                            if(err.message !== "UNAUTHORIZED"){
                                console.error(err);
                                alert(err.message || "결제 처리 중 오류가 발생했습니다.");
                            }
                            setBtnLoading(cardBtn, false);
                        });
                });
            })
            .catch(err => {
                if (err.message !== "UNAUTHORIZED") {
                    console.error(err);
                    alert(err.message || "카드 결제 준비에 실패했습니다.");
                }
                setBtnLoading(cardBtn, false);
            });
    });

    // 초기 실행
    renderPrice();
});