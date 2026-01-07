document.addEventListener('DOMContentLoaded', function() {
    
    /* --- 변수 및 요소 선택 --- */
    const modal = document.getElementById('instructor-modal');
    const btnOpenModal = document.getElementById('btn-open-instructor-modal');
    const btnCloseModal = document.querySelector('.btn-close-modal');
    const btnSearch = document.getElementById('btn-search-instructor');
    const inputSearch = document.getElementById('instructor-search-input');
    const resultList = document.getElementById('instructor-list');
    
    // 다중 선택 컨테이너
    const selectionArea = document.getElementById('instructor-selection-area');
    const inputsContainer = document.getElementById('instructor-inputs-container');
    
    // 선택된 교수자 ID 관리 (중복 방지)
    const selectedInstructorIds = new Set();

    /* --- 강의 오픈 기간 토글 로직 --- */
    const toggleAlwaysOpen = document.getElementById('toggle-always-open');
    const dateSelectionArea = document.getElementById('date-selection-area');
    const startDateInput = document.querySelector('input[name="startDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');

    if (toggleAlwaysOpen && dateSelectionArea) {
        // 초기 상태 설정
        toggleDateInputs(toggleAlwaysOpen.checked);

        toggleAlwaysOpen.addEventListener('change', function() {
            toggleDateInputs(this.checked);
        });
    }

    function toggleDateInputs(isAlwaysOpen) {
        if (isAlwaysOpen) {
            // 상시 오픈: 날짜 입력 숨김
            dateSelectionArea.style.display = 'none';
        } else {
            // 기간 설정: 날짜 입력 표시
            dateSelectionArea.style.display = 'block';
        }
    }


    /* --- 모달 제어 --- */
    // 모달 열기
    if (btnOpenModal) {
        btnOpenModal.addEventListener('click', function() {
            modal.style.display = 'flex';
            inputSearch.value = ''; // 열 때 검색어 초기화
            resultList.innerHTML = '<div class="empty-result">검색어를 입력하세요.</div>';
            inputSearch.focus();
        });
    }

    // 모달 닫기
    function closeModal() {
        modal.style.display = 'none';
    }

    if (btnCloseModal) {
        btnCloseModal.addEventListener('click', closeModal);
    }

    // 배경 클릭 시 닫기
    window.addEventListener('click', function(event) {
        if (event.target === modal) {
            closeModal();
        }
    });

    /* --- 검색 로직 --- */
    function searchInstructors() {
        const keyword = inputSearch.value.trim();
        if (!keyword) {
            alert('검색어를 입력하세요.');
            return;
        }

        // API 호출
        fetch(`/admin/course/api/instructors/search?keyword=${encodeURIComponent(keyword)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(users => {
                renderResults(users);
            })
            .catch(error => {
                console.error('Error:', error);
                resultList.innerHTML = '<div class="empty-result">오류가 발생했습니다.</div>';
            });
    }

    // 검색 버튼 클릭
    if (btnSearch) {
        btnSearch.addEventListener('click', searchInstructors);
    }

    // 엔터키 입력
    if (inputSearch) {
        inputSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault(); // 폼 제출 방지
                searchInstructors();
            }
        });
    }

    /* --- 결과 렌더링 및 선택 --- */
    function renderResults(users) {
        resultList.innerHTML = ''; // 초기화

        if (!users || users.length === 0) {
            resultList.innerHTML = '<div class="empty-result">검색 결과가 없습니다.</div>';
            return;
        }

        users.forEach(user => {
            const item = document.createElement('div');
            item.className = 'result-item';
            
            // 이미 선택된 사용자인지 확인
            const isSelected = selectedInstructorIds.has(user.userId);
            if (isSelected) {
                item.style.backgroundColor = '#f1f3f5';
                item.style.cursor = 'default';
            }

            // 사용자 정보 표시
            item.innerHTML = `
                <div class="result-info" style="${isSelected ? 'color: #adb5bd;' : ''}">
                    <strong>${user.name}</strong> 
                    <span class="result-sub">(${user.nickname})</span>
                </div>
                <div class="result-sub">ID: ${user.userId}</div>
            `;
            
            // 클릭 이벤트: 선택 처리 (이미 선택된 경우 무시)
            if (!isSelected) {
                item.addEventListener('click', function() {
                    addInstructor(user);
                });
            }

            resultList.appendChild(item);
        });
    }

    function addInstructor(user) {
        if (selectedInstructorIds.has(user.userId)) {
            return; // 이미 선택됨
        }

        // Set에 추가
        selectedInstructorIds.add(user.userId);

        // 1. 태그 UI 생성
        const tag = document.createElement('div');
        tag.className = 'instructor-tag-item';
        tag.dataset.userId = user.userId;
        tag.innerHTML = `
            <span>${user.name}</span>
            <button type="button" class="btn-remove">×</button>
        `;

        // 삭제 이벤트
        tag.querySelector('.btn-remove').addEventListener('click', function() {
            removeInstructor(user.userId, tag, input);
        });

        selectionArea.appendChild(tag);

        // 2. Hidden Input 생성
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'instructorIds'; // 배열로 받을 수 있게 이름 설정
        input.value = user.userId;
        inputsContainer.appendChild(input);

        // 모달 닫기
        closeModal();
    }

    function removeInstructor(userId, tagElement, inputElement) {
        // Set에서 제거
        selectedInstructorIds.delete(userId);
        // DOM 제거
        tagElement.remove();
        inputElement.remove();
    }

    /* --- 파일 업로드 처리 (썸네일) --- */
    const fileInputs = document.querySelectorAll('input[type="file"]');
    
    fileInputs.forEach(input => {
        // 동적으로 생성된 요소가 아닐 때만 (썸네일 등)
        if (input.closest('.file-upload-container')) {
            input.addEventListener('change', function(e) {
                const file = e.target.files[0];
                const previewId = e.target.id + '-preview';
                const previewContainer = document.getElementById(previewId);
                const fileNameSpan = previewContainer.querySelector('.file-name');
                const uploadBtn = e.target.nextElementSibling; // button.btn-upload

                if (file) {
                    fileNameSpan.textContent = file.name;
                    previewContainer.style.display = 'flex';
                    uploadBtn.style.display = 'none';
                }
            });
        }
    });

    const removeFileBtns = document.querySelectorAll('.btn-remove-file');
    removeFileBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const targetId = this.getAttribute('data-target');
            const input = document.getElementById(targetId);
            const previewContainer = this.parentElement;
            const uploadBtn = input.nextElementSibling; // button.btn-upload

            input.value = '';
            previewContainer.style.display = 'none';
            uploadBtn.style.display = 'inline-block';
        });
    });

    /* --- 커리큘럼 빌더 (섹션/챕터 추가) --- */
    const btnAddSection = document.getElementById('btn-add-section');
    const sectionListContainer = document.getElementById('section-list-container');
    let sectionCount = 0; // 섹션 인덱스 관리

    if (btnAddSection) {
        btnAddSection.addEventListener('click', function() {
            addSection();
        });
    }

    function addSection() {
        sectionCount++;
        
        const sectionItem = document.createElement('div');
        sectionItem.className = 'section-item';
        sectionItem.innerHTML = `
            <div class="section-header">
                <input type="text" name="sections[${sectionCount-1}].title" class="section-title-input" placeholder="섹션 제목을 입력하세요 (예: 섹션 1. 오리엔테이션)">
                <button type="button" class="btn-remove-section">삭제</button>
            </div>
            <div class="chapter-list">
                <!-- 챕터 아이템들이 여기에 추가됨 -->
            </div>
            <button type="button" class="btn-add-chapter">+ 챕터 추가</button>
        `;

        // 섹션 삭제 이벤트
        sectionItem.querySelector('.btn-remove-section').addEventListener('click', function() {
            if (confirm('섹션을 삭제하시겠습니까? 포함된 챕터도 모두 삭제됩니다.')) {
                sectionItem.remove();
            }
        });

        // 챕터 추가 이벤트
        const btnAddChapter = sectionItem.querySelector('.btn-add-chapter');
        const chapterList = sectionItem.querySelector('.chapter-list');
        let chapterCount = 0;

        btnAddChapter.addEventListener('click', function() {
            chapterCount++;
            addChapter(chapterList, sectionCount - 1, chapterCount - 1); // 인덱스 전달
        });

        sectionListContainer.appendChild(sectionItem);
    }

    function addChapter(container, sectionIndex, chapterIndex) {
        const chapterItem = document.createElement('div');
        chapterItem.className = 'chapter-item';
        
        // 고유 ID 생성 (파일 input 연결용)
        const fileInputId = `file-${sectionIndex}-${chapterIndex}-${Date.now()}`;

        chapterItem.innerHTML = `
            <div class="chapter-header">
                <input type="text" name="sections[${sectionIndex}].chapters[${chapterIndex}].title" class="chapter-input chapter-title" placeholder="챕터 제목 (클릭하여 상세 정보 펼치기)">
                <button type="button" class="btn-remove-chapter" title="챕터 삭제">×</button>
            </div>
            <div class="chapter-body">
                <input type="text" name="sections[${sectionIndex}].chapters[${chapterIndex}].videoUrl" class="chapter-input chapter-url" placeholder="영상 URL (Youtube, Vimeo 등)">
                
                <div class="file-attach-area">
                    <input type="file" id="${fileInputId}" name="sections[${sectionIndex}].chapters[${chapterIndex}].file" style="display: none;">
                    <button type="button" class="btn-attach-file" onclick="document.getElementById('${fileInputId}').click()">자료 첨부</button>
                    <span class="file-name-display" style="font-size: 1.2rem; color: #666; margin-left: 0.5rem;"></span>
                </div>
            </div>
        `;

        // 아코디언 토글 이벤트 (헤더 클릭 시)
        const header = chapterItem.querySelector('.chapter-header');
        header.addEventListener('click', function(e) {
            // 입력창이나 삭제 버튼 클릭 시에는 토글되지 않도록
            if (e.target.tagName === 'INPUT' || e.target.classList.contains('btn-remove-chapter')) {
                return;
            }
            chapterItem.classList.toggle('active');
        });

        // 챕터 삭제 이벤트
        chapterItem.querySelector('.btn-remove-chapter').addEventListener('click', function() {
            chapterItem.remove();
        });

        // 파일 선택 변경 이벤트 (파일명 표시)
        const fileInput = chapterItem.querySelector('input[type="file"]');
        const fileNameDisplay = chapterItem.querySelector('.file-name-display');
        
        fileInput.addEventListener('change', function(e) {
            if (e.target.files.length > 0) {
                fileNameDisplay.textContent = e.target.files[0].name;
                chapterItem.querySelector('.btn-attach-file').textContent = '파일 변경';
            } else {
                fileNameDisplay.textContent = '';
                chapterItem.querySelector('.btn-attach-file').textContent = '자료 첨부';
            }
        });

        container.appendChild(chapterItem);
    }

});