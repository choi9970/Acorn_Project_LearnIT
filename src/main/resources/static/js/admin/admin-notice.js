document.addEventListener('DOMContentLoaded', () => {

    /* =========================================================================
       [공지 목록] 삭제 버튼 확인
       - .btn-delete-notice 클래스를 가진 요소 클릭 시 confirm 창 띄움
    ========================================================================= */
    const deleteButtons = document.querySelectorAll('.btn-delete-notice');
    deleteButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            if (!confirm('정말 삭제하시겠습니까?')) {
                e.preventDefault();
            }
        });
    });

    /* =========================================================================
       [공지 등록/수정] 첨부파일 삭제 UI 처리
    ========================================================================= */
    const deleteFile = document.getElementById('deleteFile');
    const existingBox = document.getElementById('existingFileBox');
    const btnRemove = document.getElementById('btnRemoveExisting');
    const fileInput = document.getElementById('fileInput');

    if (btnRemove) {
        btnRemove.addEventListener('click', () => {
            // 1) hidden input 값 변경 (삭제 플래그)
            if (deleteFile) {
                deleteFile.value = 'true';
            }

            // 2) UI 숨김
            if (existingBox) {
                existingBox.style.display = 'none';
            }

            // 3) (선택) 사용자가 새로 선택한 파일이 있다면 초기화
            if (fileInput) {
                fileInput.value = '';
            }
        });
    }
});
