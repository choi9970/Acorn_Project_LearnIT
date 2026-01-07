document.addEventListener("DOMContentLoaded", () => {

  /* =========================================================================
     [공지 목록] 삭제 확인
     - 버튼 클릭뿐 아니라 "폼 제출" 자체를 막아야 안전함(엔터 제출도 포함)
  ========================================================================= */
  document.querySelectorAll("form").forEach((form) => {
    // 공지 삭제 폼만 타겟팅: 삭제 버튼(.btn-delete-notice)이 있는 폼만
    if (form.querySelector(".btn-delete-notice")) {
      form.addEventListener("submit", (e) => {
        if (!confirm("정말 삭제하시겠습니까?")) {
          e.preventDefault();
        }
      });
    }
  });

  /* =========================================================================
     [공지 등록/수정] 첨부파일 삭제 UI 처리
     - edit 모드에서만 기존파일 박스 + X 버튼이 존재함
  ========================================================================= */
  const deleteFile = document.getElementById("deleteFile");
  const existingBox = document.getElementById("existingFileBox");
  const btnRemove = document.getElementById("btnRemoveExisting");
  const fileInput = document.getElementById("fileInput");

  if (btnRemove) {
    btnRemove.addEventListener("click", () => {
      // 1) hidden input 값 변경(삭제 플래그)
      if (deleteFile) deleteFile.value = "true";

      // 2) UI 숨김
      if (existingBox) existingBox.style.display = "none";

      // 3) 사용자가 새로 선택한 파일이 있다면 초기화
      if (fileInput) fileInput.value = "";
    });
  }

});
