/**
 * 강의 관리 페이지 JS
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Course Manager Loaded');

    /* =========================================================================
       [강의 목록] 삭제 버튼 확인
    ========================================================================= */
    const deleteButtons = document.querySelectorAll('.btn-delete-course');
    deleteButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            if (!confirm('정말 삭제하시겠습니까?')) {
                e.preventDefault();
            }
        });
    });

    // 필요한 경우 추가적인 이벤트 리스너를 여기에 등록합니다.
    // 예: 필터 변경 시 자동 submit
    /*
    const filterSelect = document.querySelector('select[name="status"]');
    if(filterSelect) {
        filterSelect.addEventListener('change', function() {
            this.form.submit();
        });
    }
    */
});