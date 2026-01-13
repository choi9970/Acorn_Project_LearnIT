/**
 * 관리자 리뷰 페이지 - 아코디언 토글 및 필터 기능
 */

// 현재 적용 중인 필터
let commentStatusFilters = [];

// 필터 팝업 원래 위치 기억
const popupOrigins = new Map();

/**
 * 아코디언 토글 함수 (버튼 클릭용)
 * @param {HTMLElement} element - 클릭한 버튼 요소
 */
function toggleAccordion(element) {
    const reviewId = element.getAttribute('data-review-id');
    if (!reviewId) {
        return;
    }
    toggleAccordionByReviewId(reviewId);
}

/**
 * 아코디언 토글 함수 (행 클릭용)
 * @param {HTMLElement} row - 클릭한 행 요소
 */
function toggleAccordionByRow(row) {
    const reviewId = row.getAttribute('data-review-id');
    if (!reviewId) {
        return;
    }
    toggleAccordionByReviewId(reviewId);
}

/**
 * 리뷰 ID로 아코디언 토글
 * @param {string} reviewId - 리뷰 ID
 */
function toggleAccordionByReviewId(reviewId) {
    // 해당 리뷰의 아코디언 행 찾기
    const accordionRow = document.querySelector(`.accordion-row[data-review-id="${reviewId}"]`);
    if (!accordionRow) {
        return;
    }

    // 현재 활성 상태 확인
    const isActive = accordionRow.classList.contains('active');

    // 해당 아코디언만 토글 (다른 아코디언은 유지)
    if (isActive) {
        accordionRow.classList.remove('active');
    } else {
        accordionRow.classList.add('active');
    }
}

// DOM 로드 완료 후 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', function() {
    // 아코디언 토글 버튼에 이벤트 리스너 추가 (인라인 onclick 대신)
    document.querySelectorAll('.btn-toggle-accordion').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            toggleAccordion(this);
        });
    });

    // 필터 팝업 열기
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.stopPropagation();
            const which = btn.dataset.filter;
            if (which === "commentStatus") {
                toggleFilterPopup(which);
            }
        });
    });

    // 팝업 내부 클릭 시 닫히지 않게
    document.querySelectorAll(".filter-popup").forEach(p => {
        p.addEventListener("click", (e) => e.stopPropagation());
    });

    // 바깥 클릭 시 모두 닫기
    document.addEventListener("click", () => {
        closeAllFilterPopups();
    });

    // 창 리사이즈/스크롤 시 열려있는 팝업 닫기
    window.addEventListener("resize", closeAllFilterPopups);
    window.addEventListener("scroll", closeAllFilterPopups, true);

    // 초기 필터 팝업 렌더링
    renderCommentStatusFilter();
});

/**
 * 필터 팝업 열기/닫기
 */
function toggleFilterPopup(which) {
    const popup = document.getElementById("commentStatusFilterPopup");
    const btn = document.querySelector(`.filter-btn[data-filter="${which}"]`);

    if (!popup) return;

    // 다른 팝업은 닫고
    document.querySelectorAll(".filter-popup").forEach(p => {
        if (p !== popup) hidePopup(p);
    });

    const isOpen = popup.style.display === "block";
    if (isOpen) hidePopup(popup);
    else showPopup(popup, btn);
}

/**
 * 필터 팝업 표시
 */
function showPopup(popup, anchorBtn) {
    if (!popup || !anchorBtn) return;

    // 원래 부모/자리 기억(1회)
    if (!popupOrigins.has(popup)) {
        popupOrigins.set(popup, {
            parent: popup.parentNode,
            next: popup.nextSibling
        });
    }

    // 먼저 보이게 해서 width 계산
    popup.style.display = "block";

    const rect = anchorBtn.getBoundingClientRect();

    // body로 옮겨서(부모의 overflow/opacity 영향 끊기)
    document.body.appendChild(popup);

    // fixed 배치
    popup.style.position = "fixed";
    popup.style.top = `${rect.bottom + 8}px`;

    const w = popup.offsetWidth || 240;

    // 버튼 오른쪽 기준 정렬 + 화면 밖 방지(clamp)
    let left = rect.right - w;
    left = Math.max(8, Math.min(left, window.innerWidth - w - 8));

    popup.style.left = `${left}px`;
    popup.style.right = "auto";
    popup.style.zIndex = "999999";
}

/**
 * 필터 팝업 숨기기
 */
function hidePopup(popup) {
    if (!popup) return;

    popup.style.display = "none";
    popup.style.position = "";
    popup.style.top = "";
    popup.style.left = "";
    popup.style.right = "";
    popup.style.zIndex = "";

    const origin = popupOrigins.get(popup);
    if (origin?.parent) {
        origin.parent.insertBefore(popup, origin.next);
    }
}

/**
 * 모든 필터 팝업 닫기
 */
function closeAllFilterPopups() {
    document.querySelectorAll(".filter-popup").forEach(p => hidePopup(p));
}

/**
 * 댓글 상태 필터 팝업 렌더링
 */
function renderCommentStatusFilter() {
    const popup = document.getElementById("commentStatusFilterPopup");
    if (!popup) return;

    // 현재 URL에서 commentStatus 파라미터 읽기
    const urlParams = new URLSearchParams(window.location.search);
    const currentStatus = urlParams.get('commentStatus') || '';
    
    // 단일 선택이므로 배열이 아닌 단일 값으로 처리
    const selectedStatus = currentStatus ? [currentStatus] : [];

    const statusOptions = [
        { value: '', label: '전체' },
        { value: 'Active', label: 'Active' },
        { value: 'Approved', label: 'Approved' },
        { value: 'Rejected', label: 'Rejected' }
    ];

    popup.innerHTML = `
        <div class="filter-title">댓글 상태</div>
        <div class="filter-list">
            ${statusOptions.map(opt => `
                <label>
                    <input type="radio" name="commentStatus" value="${opt.value}" 
                           ${selectedStatus.includes(opt.value) ? 'checked' : ''}>
                    <span>${opt.label}</span>
                </label>
            `).join('')}
        </div>
        <div class="filter-actions">
            <button type="button" onclick="applyCommentStatusFilter()" class="primary">적용</button>
            <button type="button" onclick="clearCommentStatusFilter()">초기화</button>
        </div>
    `;

    // 필터 버튼 상태 업데이트
    updateFilterButtonState();
}

/**
 * 댓글 상태 필터 적용
 */
function applyCommentStatusFilter() {
    const selected = document.querySelector('input[name="commentStatus"]:checked');
    const status = selected ? selected.value : '';
    
    const url = new URL(window.location.href);
    if (status) {
        url.searchParams.set('commentStatus', status);
    } else {
        url.searchParams.delete('commentStatus');
    }
    url.searchParams.set('page', '1'); // 필터 변경 시 첫 페이지로
    
    window.location.href = url.toString();
}

/**
 * 댓글 상태 필터 초기화
 */
function clearCommentStatusFilter() {
    const url = new URL(window.location.href);
    url.searchParams.delete('commentStatus');
    url.searchParams.set('page', '1');
    
    window.location.href = url.toString();
}

/**
 * 필터 버튼 상태 업데이트
 */
function updateFilterButtonState() {
    const urlParams = new URLSearchParams(window.location.search);
    const currentStatus = urlParams.get('commentStatus');
    const btn = document.querySelector('.filter-btn[data-filter="commentStatus"]');
    
    if (btn) {
        btn.classList.toggle("filter-on", !!currentStatus);
    }
}

