document.addEventListener('click', async (e) => {
    if (e.target.classList.contains('receipt-btn')) {
        const paymentId = e.target.dataset.id;
        const res = await fetch(`/mypage/purchase/${paymentId}/receipt`);
        const data = await res.json();

        renderReceipt(data);
        openReceipt();
    }
});

function renderReceipt(r) {
    const body = document.getElementById("receiptBody");
    body.innerHTML = `
        <div class="receipt-header">
            <span class="status">${r.paymentStatusDesc}</span>
            <span>주문번호 · ${r.orderNo}</span>
        </div>
        <p class="receipt-date">결제일시 · ${new Date(r.paidAt).toLocaleString()}</p>
        <p class="receipt-method">결제 수단 · ${r.paymentMethodDesc}</p>
        <ul class="receipt-courses">
            ${r.courses.map(c => `
                <li>
                    <span>${c.courseTitle}</span>
                    <strong>${c.price.toLocaleString()}원</strong>
                </li>`).join('')}
        </ul>
        <hr>
        <ul class="receipt-prices">
            <li><span>금액</span><strong>${r.originAmount.toLocaleString()}원</strong></li>
            <li><span>할인</span><strong>- ${r.discountAmount.toLocaleString()}원</strong></li>
            <li class="total"><span>최종 결제 금액</span><strong>${r.totalAmount.toLocaleString()}원</strong></li>
        </ul>
    `;
}

function openReceipt() {
    document.getElementById('receiptModal').style.display = 'flex';
}

function closeReceipt() {
    document.getElementById('receiptModal').style.display = 'none';
}

// PDF 다운로드 함수
function downloadReceipt() {
    const receipt = document.querySelector(".receipt-modal");
    if (!receipt || !receipt.innerText.trim()) {
        alert("데이터가 로드되지 않았습니다.");
        return;
    }

    const opt = {
        margin: 10,
        filename: '결제영수증.pdf',
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: {
            scale: 2,
            useCORS: true,
            backgroundColor: "#ffffff",
            width: 520
        },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' }
    };

    html2pdf().set(opt).from(receipt).save();
}