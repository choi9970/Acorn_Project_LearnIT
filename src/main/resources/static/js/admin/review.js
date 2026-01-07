/**
 * 관리자 리뷰 페이지 - 아코디언 토글 기능
 */

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
});

