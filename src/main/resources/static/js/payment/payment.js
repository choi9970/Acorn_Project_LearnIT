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


    // 금액 포맷 함수
    function formatWon(n) {
        return n.toLocaleString("ko-KR") + "원";
    }

    // 화면 갱신
    function renderPrice() {
        const couponCard = document.querySelector(".payment-left .card:nth-child(2)");
        let isBtnDisabled = false;
        let currentDiscount = 0;

        // 1. 무료 강의 처리
        if (basePrice === 0) {
            if (couponSelect) {
                couponSelect.disabled = true;
                couponSelect.options[0].textContent = "무료 강의는 쿠폰을 사용할 수 없습니다.";
                couponSelect.value = "";
            }
            if (couponCard) couponCard.classList.add("is-disabled");
            warningText.style.display = "none";
            discountAmount = 0;
            selectedCouponId = null;
        }
        // 2. 유료 강의 처리
        else {
            if (selectedCouponId) {
                const opt = couponSelect.selectedOptions[0];
                const minPrice = Number(opt.dataset.minPrice || 0);
                const couponDiscount = Number(opt.dataset.discount || 0);

                if ((basePrice - couponDiscount) < minPrice) {
                    warningText.textContent = `할인 후 결제 금액이 최소 ${minPrice.toLocaleString()}원 이상이어야 이 쿠폰을 사용할 수 있습니다.`;
                    warningText.style.display = "flex";
                    couponSelect.classList.add("invalid");

                    isBtnDisabled = true;
                    currentDiscount = 0;
                } else {
                    warningText.style.display = "none";
                    couponSelect.classList.remove("invalid");
                    currentDiscount = couponDiscount;
                }
            } else {
                warningText.style.display = "none";
                currentDiscount = 0;
            }
        }

        discountAmount = currentDiscount;

        const finalPrice = Math.max(0, basePrice - discountAmount);

        totalText.textContent = formatWon(basePrice);
        discountText.textContent = "-" + formatWon(discountAmount);
        finalText.textContent = formatWon(finalPrice);

        kakaoBtn.disabled = isBtnDisabled;
        if (cardBtn) cardBtn.disabled = isBtnDisabled;

        if (finalPrice === 0) {
            kakaoBtn.textContent = "무료로 수강하기";
            if (cardBtn) cardBtn.style.display = "none";
        } else {
            kakaoBtn.textContent = "카카오페이로 결제";
            if (cardBtn) cardBtn.style.display = "block";
        }

    }


    // 쿠폰 선택 이벤트
    couponSelect?.addEventListener("change", (e) => {
        selectedCouponId = e.target.value || null;
        renderPrice();
    });

    // 로딩 상태 제어
    function setBtnLoading(btn, loading) {
        btn.disabled = loading;
        btn.setAttribute("aria-busy", String(loading));
    }

    //응답 형태 처리
    async function checkResponse(res) {
        if (res.status === 401) {
            alert("로그인이 필요합니다.");
            location.href = "/login";
            throw new Error("UNAUTHORIZED");
        }
        const contentType = res.headers.get("content-type");
        if (!res.ok || !contentType || !contentType.includes("application/json")) {
            // 서버에서 HTML 에러 페이지가 올 경우를 대비
            const text = await res.text();
            throw new Error(text || "서버 통신 오류가 발생했습니다.");
        }
        return res.json();
    }

    //무료 결제 처리
    async function handleFreePayment(){
        if(!confirm("바로 수강신청하시겠습니까?")) return;

        try{
            const res = await fetch("/payments/free/complete", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({
                    courseIds: courseIds,
                    totalPrice: 0,
                    couponId: null
                })
            });
            const data = await checkResponse(res);
            location.href = "/payment/result?orderNo=" + data.orderNo;

        }catch (e) {
            alert(e.message);
        }
    }

    // 카카오페이 결제
    kakaoBtn?.addEventListener("click", () => {
        const finalPrice = Math.max(0, basePrice - discountAmount);

        if(basePrice === 0 || finalPrice === 0){
            handleFreePayment();
        }else{
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
                    totalPrice: finalPrice,
                    couponId: selectedCouponId
                })
            })
                .then(checkResponse)
                .then(data => location.href = data.next_redirect_pc_url)
                .catch(err => {
                    if(err.message !== "UNAUTHORIZED") alert(err.message);
                    setBtnLoading(kakaoBtn, false);
                });
        }

    });

    // 일반 카드 결제
    cardBtn?.addEventListener("click", () => {
        const finalPrice = Math.max(0, basePrice - discountAmount);

        if(basePrice === 0 || finalPrice === 0){
            handleFreePayment();
        }else{
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
                        name: "learnIT 강의 결제",
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
        }

    });

    // 초기 실행
    renderPrice();
});