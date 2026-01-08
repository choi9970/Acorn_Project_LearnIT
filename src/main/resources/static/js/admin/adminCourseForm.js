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
    
    // [Edit Mode] 초기화: 기존 선택된 강사가 있다면 Set에 추가 및 이벤트 연결
    document.querySelectorAll('.instructor-tag-item').forEach(item => {
        const userId = parseInt(item.dataset.userId); 
        if (userId) {
            selectedInstructorIds.add(userId);
            
            const btnRemove = item.querySelector('.btn-remove');
            if (btnRemove) {
                btnRemove.addEventListener('click', function() {
                    const input = document.querySelector(`input[name="instructorIds"][value="${userId}"]`);
                    removeInstructor(userId, item, input);
                });
            }
        }
    });

    /* --- 강의 오픈 기간 토글 로직 --- */
    const toggleAlwaysOpen = document.getElementById('toggle-always-open');
    const dateSelectionArea = document.getElementById('date-selection-area');

    if (toggleAlwaysOpen && dateSelectionArea) {
        toggleDateInputs(toggleAlwaysOpen.checked);

        toggleAlwaysOpen.addEventListener('change', function() {
            toggleDateInputs(this.checked);
        });
    }

    function toggleDateInputs(isAlwaysOpen) {
        if (isAlwaysOpen) {
            dateSelectionArea.style.display = 'none';
        } else {
            dateSelectionArea.style.display = 'block';
        }
    }


    /* --- 모달 제어 --- */
    // 모달 열기
    if (btnOpenModal) {
        btnOpenModal.addEventListener('click', function() {
            modal.style.display = 'flex';
            inputSearch.value = ''; 
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

    if (btnSearch) {
        btnSearch.addEventListener('click', searchInstructors);
    }

    if (inputSearch) {
        inputSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault(); 
                searchInstructors();
            }
        });
    }

    /* --- 결과 렌더링 및 선택 --- */
    function renderResults(users) {
        resultList.innerHTML = ''; 

        if (!users || users.length === 0) {
            resultList.innerHTML = '<div class="empty-result">검색 결과가 없습니다.</div>';
            return;
        }

        users.forEach(user => {
            const item = document.createElement('div');
            item.className = 'result-item';
            
            const isSelected = selectedInstructorIds.has(user.userId);
            if (isSelected) {
                item.style.backgroundColor = '#f1f3f5';
                item.style.cursor = 'default';
            }

            item.innerHTML = `
                <div class="result-info" style="${isSelected ? 'color: #adb5bd;' : ''}">
                    <strong>${user.name}</strong> 
                    <span class="result-sub">(${user.nickname})</span>
                </div>
                <div class="result-sub">ID: ${user.userId}</div>
            `;
            
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
            return; 
        }

        selectedInstructorIds.add(user.userId);

        const tag = document.createElement('div');
        tag.className = 'instructor-tag-item';
        tag.dataset.userId = user.userId;
        tag.innerHTML = `
            <span>${user.name}</span>
            <button type="button" class="btn-remove">×</button>
        `;

        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'instructorIds'; 
        input.value = user.userId;
        inputsContainer.appendChild(input);

        tag.querySelector('.btn-remove').addEventListener('click', function() {
            removeInstructor(user.userId, tag, input);
        });

        selectionArea.appendChild(tag);
        closeModal();
    }

    function removeInstructor(userId, tagElement, inputElement) {
        selectedInstructorIds.delete(userId);
        tagElement.remove();
        if (inputElement) inputElement.remove();
    }

    /* --- 파일 업로드 처리 (썸네일) --- */
    const uploadBtns = document.querySelectorAll('.btn-upload');
    uploadBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const targetId = this.getAttribute('data-target');
            if (targetId) {
                const input = document.getElementById(targetId);
                if (input) input.click();
            }
        });
    });

    const fileInputs = document.querySelectorAll('input[type="file"]');
    fileInputs.forEach(input => {
        if (input.closest('.file-upload-container')) {
            input.addEventListener('change', function(e) {
                const file = e.target.files[0];
                const previewId = e.target.id + '-preview';
                const previewContainer = document.getElementById(previewId);
                const fileNameSpan = previewContainer.querySelector('.file-name');
                const uploadBtn = e.target.nextElementSibling; 

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
            const uploadBtn = input.nextElementSibling; 

            input.value = '';
            previewContainer.style.display = 'none';
            uploadBtn.style.display = 'inline-block';
        });
    });

    /* --- 커리큘럼 빌더 (섹션/챕터/퀴즈 추가) --- */
    const btnAddSection = document.getElementById('btn-add-section');
    const sectionListContainer = document.getElementById('section-list-container');
    
    let sectionCount = document.querySelectorAll('.section-item').length; 

    document.querySelectorAll('.section-item').forEach((sectionItem, sIdx) => {
        setupSectionEvents(sectionItem, sIdx);
    });

    if (btnAddSection) {
        btnAddSection.addEventListener('click', function() {
            addSection();
        });
    }

    function addSection() {
        const currentSectionIndex = sectionCount++;
        
        const sectionItem = document.createElement('div');
        sectionItem.className = 'section-item active'; 
        sectionItem.innerHTML = `
            <div class="section-header">
                <input type="text" name="sections[${currentSectionIndex}].title" class="section-title-input" placeholder="섹션 제목을 입력하세요 (예: 섹션 1. 오리엔테이션)">
                <button type="button" class="btn-remove-section">삭제</button>
            </div>
            <div class="section-body">
                <div class="chapter-list"></div>
                <button type="button" class="btn-add-chapter" data-section-index="${currentSectionIndex}">+ 챕터 추가</button>
                <div class="quiz-list"></div>
                <button type="button" class="btn-add-quiz" data-section-index="${currentSectionIndex}">+ 퀴즈 추가</button>
            </div>
        `;
        
        sectionListContainer.appendChild(sectionItem);
        setupSectionEvents(sectionItem, currentSectionIndex);
    }

    function setupSectionEvents(sectionItem, sectionIndex) {
        const header = sectionItem.querySelector('.section-header');
        if (header) {
            header.addEventListener('click', function(e) {
                if (e.target.tagName === 'INPUT' || e.target.tagName === 'BUTTON' || e.target.classList.contains('btn-remove-section')) {
                    return;
                }
                sectionItem.classList.toggle('active');
            });
        }

        const btnRemoveSection = sectionItem.querySelector('.btn-remove-section');
        if (btnRemoveSection) {
            btnRemoveSection.addEventListener('click', function() {
                if (confirm('섹션을 삭제하시겠습니까? 포함된 챕터와 퀴즈도 모두 삭제됩니다.')) {
                    sectionItem.remove();
                }
            });
        }

        const btnAddChapter = sectionItem.querySelector('.btn-add-chapter');
        const chapterList = sectionItem.querySelector('.chapter-list');
        if (btnAddChapter) {
            btnAddChapter.addEventListener('click', function() {
                let maxIdx = -1;
                chapterList.querySelectorAll('.chapter-item').forEach(ch => {
                    const chInput = ch.querySelector('.chapter-title');
                    const match = chInput.name.match(/chapters\[(\d+)\]/);
                    if (match) {
                        const idx = parseInt(match[1]);
                        if (idx > maxIdx) maxIdx = idx;
                    }
                });
                addChapter(chapterList, sectionIndex, maxIdx + 1);
            });
        }

        const btnAddQuiz = sectionItem.querySelector('.btn-add-quiz');
        const quizList = sectionItem.querySelector('.quiz-list');
        if (btnAddQuiz) {
            btnAddQuiz.addEventListener('click', function() {
                 let maxIdx = -1;
                quizList.querySelectorAll('.quiz-item').forEach(q => {
                    const qInput = q.querySelector('.quiz-title');
                    const match = qInput.name.match(/quizzes\[(\d+)\]/);
                    if (match) {
                        const idx = parseInt(match[1]);
                        if (idx > maxIdx) maxIdx = idx;
                    }
                });
                addQuiz(quizList, `sections[${sectionIndex}].quizzes[${maxIdx + 1}]`);
            });
        }
        
        sectionItem.querySelectorAll('.chapter-item').forEach(chapterItem => {
            setupChapterEvents(chapterItem);
        });

        sectionItem.querySelectorAll('.quiz-item').forEach(quizItem => {
            setupQuizEvents(quizItem);
        });
    }

    function addChapter(container, sectionIndex, chapterIndex) {
        const chapterItem = document.createElement('div');
        chapterItem.className = 'chapter-item active'; 
        
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

        container.appendChild(chapterItem);
        setupChapterEvents(chapterItem);
    }

    function setupChapterEvents(chapterItem) {
        const header = chapterItem.querySelector('.chapter-header');
        header.addEventListener('click', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.classList.contains('btn-remove-chapter')) {
                return;
            }
            chapterItem.classList.toggle('active');
        });

        chapterItem.querySelector('.btn-remove-chapter').addEventListener('click', function() {
            chapterItem.remove();
        });

        const fileInput = chapterItem.querySelector('input[type="file"]');
        const fileNameDisplay = chapterItem.querySelector('.file-name-display');
        
        if (fileInput) {
            fileInput.addEventListener('change', function(e) {
                if (e.target.files.length > 0) {
                    fileNameDisplay.textContent = e.target.files[0].name;
                    chapterItem.querySelector('.btn-attach-file').textContent = '파일 변경';
                } else {
                    if (!chapterItem.querySelector('input[name*="existingFileName"]')?.value) {
                         fileNameDisplay.textContent = '';
                         chapterItem.querySelector('.btn-attach-file').textContent = '자료 첨부';
                    }
                }
            });
        }
    }

    /* --- 퀴즈 관련 JS --- */

    function addQuiz(container, prefix) {
        const quizItem = document.createElement('div');
        quizItem.className = 'quiz-item active'; 
        
        quizItem.innerHTML = `
            <div class="quiz-header">
                <span class="badge-quiz">QUIZ</span>
                <input type="text" name="${prefix}.title" class="quiz-input quiz-title" placeholder="퀴즈 제목">
                <button type="button" class="btn-remove-quiz">×</button>
            </div>
            <div class="quiz-body">
                <div class="question-list"></div>
                <button type="button" class="btn-add-question">+ 문제 추가</button>
            </div>
        `;

        container.appendChild(quizItem);
        setupQuizEvents(quizItem); 
    }
    
    function setupQuizEvents(quizItem) {
         const header = quizItem.querySelector('.quiz-header');
         header.addEventListener('click', function(e) {
             if (e.target.tagName === 'INPUT' || e.target.tagName === 'BUTTON' || e.target.closest('button')) {
                 return;
             }
             quizItem.classList.toggle('active');
         });

         const btnRemove = quizItem.querySelector('.btn-remove-quiz');
         if (btnRemove) {
             btnRemove.addEventListener('click', function() {
                 if (confirm('퀴즈를 삭제하시겠습니까?')) {
                     quizItem.remove();
                 }
             });
         }

         const btnAddQuestion = quizItem.querySelector('.btn-add-question');
         const questionList = quizItem.querySelector('.question-list');
         
         const titleInput = quizItem.querySelector('.quiz-title');
         const prefixMatch = titleInput.name.match(/(.*)\.title/);
         const quizPrefix = prefixMatch ? prefixMatch[1] : '';

         if (btnAddQuestion && quizPrefix) {
             btnAddQuestion.addEventListener('click', function() {
                 let maxIdx = -1;
                 questionList.querySelectorAll('.question-item').forEach(q => {
                     const qInput = q.querySelector('.question-input');
                     const match = qInput.name.match(/questions\[(\d+)\]/);
                     if (match) {
                         const idx = parseInt(match[1]);
                         if (idx > maxIdx) maxIdx = idx;
                     }
                 });
                 addQuestion(questionList, `${quizPrefix}.questions[${maxIdx + 1}]`);
             });
         }

         quizItem.querySelectorAll('.question-item').forEach(questionItem => {
             setupQuestionEvents(questionItem);
         });
    }

    function addQuestion(container, prefix) {
        const questionItem = document.createElement('div');
        questionItem.className = 'question-item';
        
        questionItem.innerHTML = `
            <div class="question-header">
                <input type="text" name="${prefix}.content" class="question-input" placeholder="문제 내용">
                <button type="button" class="btn-remove-question">×</button>
            </div>
            <div class="question-body">
                <textarea name="${prefix}.explanation" class="question-explanation" placeholder="해설을 입력하세요"></textarea>
                <div class="option-list"></div>
                <button type="button" class="btn-add-option">+ 보기 추가</button>
            </div>
        `;
        
        container.appendChild(questionItem);
        setupQuestionEvents(questionItem);
    }

    function setupQuestionEvents(questionItem) {
        questionItem.querySelector('.btn-remove-question').addEventListener('click', function() {
            questionItem.remove();
        });

        const btnAddOption = questionItem.querySelector('.btn-add-option');
        const optionList = questionItem.querySelector('.option-list');
        
        const contentInput = questionItem.querySelector('.question-input');
        const prefixMatch = contentInput.name.match(/(.*)\.content/);
        const questionPrefix = prefixMatch ? prefixMatch[1] : '';

        if (btnAddOption && questionPrefix) {
            btnAddOption.addEventListener('click', function() {
                let maxIdx = -1;
                optionList.querySelectorAll('.option-item').forEach(o => {
                    const oInput = o.querySelector('.option-input');
                    const match = oInput.name.match(/options\[(\d+)\]/);
                    if (match) {
                        const idx = parseInt(match[1]);
                        if (idx > maxIdx) maxIdx = idx;
                    }
                });
                addOption(optionList, `${questionPrefix}.options[${maxIdx + 1}]`, questionPrefix);
            });
        }
        
        questionItem.querySelectorAll('.option-item').forEach(optionItem => {
            setupOptionEvents(optionItem, questionPrefix);
        });
    }

    function addOption(container, prefix, questionPrefix) {
        const optionItem = document.createElement('div');
        optionItem.className = 'option-item';
        
        const optionIndexMatch = prefix.match(/options\[(\d+)\]/);
        const optionIndex = optionIndexMatch ? optionIndexMatch[1] : Date.now();
        
        optionItem.innerHTML = `
            <input type="radio" name="${questionPrefix}.correctOptionIndex" value="${optionIndex}">
            <input type="hidden" name="${prefix}.isCorrect" class="is-correct-input" value="N">
            <input type="text" name="${prefix}.content" class="option-input" placeholder="보기 내용">
            <button type="button" class="btn-remove-option">×</button>
        `;
        
        container.appendChild(optionItem);
        setupOptionEvents(optionItem, questionPrefix);
    }
    
    function setupOptionEvents(optionItem, questionPrefix) {
        optionItem.querySelector('.btn-remove-option').addEventListener('click', function() {
            optionItem.remove();
        });
        
        const radio = optionItem.querySelector('input[type="radio"]');
        
        radio.addEventListener('change', function() {
             const optionList = optionItem.closest('.option-list');
             optionList.querySelectorAll('.option-item').forEach(item => {
                 const hidden = item.querySelector('.is-correct-input');
                 const r = item.querySelector('input[type="radio"]');
                 if (r.checked) {
                     hidden.value = 'Y';
                 } else {
                     hidden.value = 'N';
                 }
             });
        });
    }

    /* --- 파이널 퀴즈 --- */
    const btnCreateFinalQuiz = document.getElementById('btn-create-final-quiz');
    const finalQuizContainer = document.getElementById('final-quiz-container');
    
    if (btnCreateFinalQuiz) {
        btnCreateFinalQuiz.addEventListener('click', function() {
            btnCreateFinalQuiz.parentElement.remove();
            
            const quizItem = document.createElement('div');
            quizItem.className = 'quiz-item final-quiz-item active'; 
            quizItem.innerHTML = `
                <div class="quiz-header">
                    <span class="badge-quiz final">FINAL</span>
                    <input type="text" name="finalQuiz.title" class="quiz-input quiz-title" placeholder="파이널 퀴즈 제목">
                    <button type="button" class="btn-remove-quiz">×</button>
                </div>
                <div class="quiz-body">
                    <div class="question-list"></div>
                    <button type="button" class="btn-add-question">+ 문제 추가</button>
                </div>
            `;
            
            finalQuizContainer.appendChild(quizItem);
            
            quizItem.querySelector('.btn-remove-quiz').addEventListener('click', function() {
                if (confirm('파이널 퀴즈를 삭제하시겠습니까?')) {
                    finalQuizContainer.innerHTML = `
                        <div class="empty-final-quiz">
                            <button type="button" id="btn-create-final-quiz" class="btn-add-quiz final">+ 파이널 퀴즈 생성</button>
                        </div>
                    `;
                    document.getElementById('btn-create-final-quiz').addEventListener('click', createFinalQuiz);
                }
            });
            
            setupQuizEvents(quizItem);
        });
    }

    function createFinalQuiz() {
         const container = document.getElementById('final-quiz-container');
         container.innerHTML = '';
         
         const quizItem = document.createElement('div');
         quizItem.className = 'quiz-item final-quiz-item active'; 
         quizItem.innerHTML = `
            <div class="quiz-header">
                <span class="badge-quiz final">FINAL</span>
                <input type="text" name="finalQuiz.title" class="quiz-input quiz-title" placeholder="파이널 퀴즈 제목">
                <button type="button" class="btn-remove-quiz">×</button>
            </div>
            <div class="quiz-body">
                <div class="question-list"></div>
                <button type="button" class="btn-add-question">+ 문제 추가</button>
            </div>
         `;
         container.appendChild(quizItem);
         setupQuizEvents(quizItem);
         
         quizItem.querySelector('.btn-remove-quiz').addEventListener('click', function() {
            if (confirm('파이널 퀴즈를 삭제하시겠습니까?')) {
                resetFinalQuizBtn();
            }
         });
    }

    function resetFinalQuizBtn() {
        const container = document.getElementById('final-quiz-container');
        container.innerHTML = `
            <div class="empty-final-quiz">
                <button type="button" id="btn-create-final-quiz" class="btn-add-quiz final">+ 파이널 퀴즈 생성</button>
            </div>
        `;
        document.getElementById('btn-create-final-quiz').addEventListener('click', createFinalQuiz);
    }
    
    if (btnCreateFinalQuiz) {
        const clone = btnCreateFinalQuiz.cloneNode(true);
        btnCreateFinalQuiz.parentNode.replaceChild(clone, btnCreateFinalQuiz);
        clone.addEventListener('click', createFinalQuiz);
    }
    
    const existingFinalQuiz = document.querySelector('.final-quiz-item');
    if (existingFinalQuiz) {
        setupQuizEvents(existingFinalQuiz);
        existingFinalQuiz.querySelector('.btn-remove-quiz').addEventListener('click', function() {
             if (confirm('파이널 퀴즈를 삭제하시겠습니까?')) {
                resetFinalQuizBtn();
            }
        });
    }

    /* --- Form Validation --- */
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function(e) {
            const title = form.querySelector('input[name="title"]');
            if (!title.value.trim()) {
                alert('강의 제목을 입력하세요.');
                title.focus();
                e.preventDefault();
                return;
            }
            
            const category = form.querySelector('select[name="categoryId"]');
            if (!category.value) {
                alert('카테고리를 선택하세요.');
                category.focus();
                e.preventDefault();
                return;
            }
            
            const price = form.querySelector('input[name="price"]');
            if (!price.value) {
                alert('가격을 입력하세요.');
                price.focus();
                e.preventDefault();
                return;
            }

            const sections = document.querySelectorAll('.section-item');
            if (sections.length === 0) {
                alert('최소 1개 이상의 섹션이 필요합니다.');
                e.preventDefault();
                return;
            }

            for (let i = 0; i < sections.length; i++) {
                const section = sections[i];
                const sectionTitle = section.querySelector('.section-title-input');
                
                if (!sectionTitle.value.trim()) {
                    alert(`섹션 ${i + 1}의 제목을 입력하세요.`);
                    sectionTitle.focus();
                    e.preventDefault();
                    return;
                }

                const chapters = section.querySelectorAll('.chapter-item');
                if (chapters.length === 0) {
                    alert(`섹션 ${i + 1}에 최소 1개 이상의 챕터가 필요합니다.`);
                    e.preventDefault();
                    return;
                }

                for (let j = 0; j < chapters.length; j++) {
                    const chapter = chapters[j];
                    const chapterTitle = chapter.querySelector('.chapter-title');
                    const videoUrl = chapter.querySelector('.chapter-url');

                    if (!chapterTitle.value.trim()) {
                        alert(`섹션 ${i + 1}, 챕터 ${j + 1}의 제목을 입력하세요.`);
                        if (!chapter.classList.contains('active')) chapter.classList.add('active');
                        chapterTitle.focus();
                        e.preventDefault();
                        return;
                    }
                    if (!videoUrl.value.trim()) {
                        alert(`섹션 ${i + 1}, 챕터 ${j + 1}의 영상 URL을 입력하세요.`);
                        if (!chapter.classList.contains('active')) chapter.classList.add('active');
                        videoUrl.focus();
                        e.preventDefault();
                        return;
                    }
                }
                
                const quizzes = section.querySelectorAll('.quiz-item');
                for (let k = 0; k < quizzes.length; k++) {
                    if (!validateQuiz(quizzes[k], `섹션 ${i + 1}의 퀴즈 ${k + 1}`)) {
                        e.preventDefault();
                        return;
                    }
                }
            }
            
            const finalQuiz = document.querySelector('.final-quiz-item');
            if (finalQuiz) {
                if (!validateQuiz(finalQuiz, '파이널 퀴즈')) {
                    e.preventDefault();
                    return;
                }
            }
        });
    }

    function validateQuiz(quizItem, quizName) {
        const title = quizItem.querySelector('.quiz-title');
        if (!title.value.trim()) {
            alert(`${quizName}의 제목을 입력하세요.`);
            if (!quizItem.classList.contains('active')) quizItem.classList.add('active');
            title.focus();
            return false;
        }

        const questions = quizItem.querySelectorAll('.question-item');
        if (questions.length === 0) {
            alert(`${quizName}에 최소 1개 이상의 문제가 필요합니다.`);
            if (!quizItem.classList.contains('active')) quizItem.classList.add('active');
            return false;
        }

        for (let i = 0; i < questions.length; i++) {
            const q = questions[i];
            const content = q.querySelector('.question-input');
            const explanation = q.querySelector('.question-explanation');
            const options = q.querySelectorAll('.option-item');
            
            if (!content.value.trim()) {
                alert(`${quizName}, 문제 ${i + 1}의 내용을 입력하세요.`);
                if (!quizItem.classList.contains('active')) quizItem.classList.add('active');
                content.focus();
                return false;
            }
            
            if (!explanation.value.trim()) {
                alert(`${quizName}, 문제 ${i + 1}의 해설을 입력하세요.`);
                if (!quizItem.classList.contains('active')) quizItem.classList.add('active');
                explanation.focus();
                return false;
            }

            if (options.length < 2) {
                alert(`${quizName}, 문제 ${i + 1}에 최소 2개 이상의 선택지가 필요합니다.`);
                if (!quizItem.classList.contains('active')) quizItem.classList.add('active');
                return false;
            }

            let hasCorrect = false;
            for (let j = 0; j < options.length; j++) {
                const opt = options[j];
                const optContent = opt.querySelector('.option-input');
                const radio = opt.querySelector('input[type="radio"]');
                
                if (!optContent.value.trim()) {
                    alert(`${quizName}, 문제 ${i + 1}의 선택지 ${j + 1} 내용을 입력하세요.`);
                    if (!quizItem.classList.contains('active')) quizItem.classList.add('active');
                    optContent.focus();
                    return false;
                }
                
                if (radio.checked) hasCorrect = true;
            }

            if (!hasCorrect) {
                alert(`${quizName}, 문제 ${i + 1}의 정답을 선택하세요.`);
                if (!quizItem.classList.contains('active')) quizItem.classList.add('active');
                return false;
            }
        }
        return true;
    }

});