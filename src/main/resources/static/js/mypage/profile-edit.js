// 개인정보 수정 페이지 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    const editButtons = document.querySelectorAll('.btn-edit');
    const form = document.getElementById('profile-edit-form');
    
    // 프로필 이미지 업로드 처리
    const profileImageInput = document.getElementById('profile-image-input');
    const profileImagePreview = document.getElementById('profile-image-preview');
    const btnUploadImage = document.getElementById('btn-upload-image');
    const btnRemoveImage = document.getElementById('btn-remove-image');
    
    // 현재 이미지가 기본 이미지가 아닌 경우 제거 버튼 표시
    if (profileImagePreview && btnRemoveImage) {
        const currentSrc = profileImagePreview.src;
        if (currentSrc && !currentSrc.includes('logo_icon.png')) {
            btnRemoveImage.style.display = 'block';
            // 원본 이미지 URL 저장
            profileImagePreview.setAttribute('data-original-src', currentSrc);
        }
    }
    
    if (btnUploadImage && profileImageInput) {
        btnUploadImage.addEventListener('click', function() {
            profileImageInput.click();
        });
    }
    
    if (profileImageInput) {
        profileImageInput.addEventListener('change', function(event) {
            handleImageUpload(event);
        });
    }
    
    if (btnRemoveImage) {
        btnRemoveImage.addEventListener('click', function() {
            removeProfileImage();
        });
    }
    
    /**
     * 이미지 업로드 처리
     */
    function handleImageUpload(event) {
        const file = event.target.files[0];
        if (!file) return;
        
        // 파일 검증 (크기, 형식)
        if (file.size > 5 * 1024 * 1024) {
            alert('파일 크기는 5MB 이하여야 합니다.');
            event.target.value = ''; // 파일 선택 초기화
            return;
        }
        
        if (!file.type.startsWith('image/')) {
            alert('이미지 파일만 업로드 가능합니다.');
            event.target.value = ''; // 파일 선택 초기화
            return;
        }
        
        // 미리보기
        const reader = new FileReader();
        reader.onload = function(e) {
            profileImagePreview.src = e.target.result;
            btnRemoveImage.style.display = 'block';
        };
        reader.readAsDataURL(file);
        
        // FormData로 서버에 전송
        const formData = new FormData();
        formData.append('profileImage', file);
        
        // 업로드 중 표시
        btnUploadImage.disabled = true;
        btnUploadImage.textContent = '업로드 중...';
        
        fetch('/mypage/settings/upload-profile-image', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('프로필 이미지가 변경되었습니다.');
                // 이미지 URL 업데이트
                if (data.imageUrl) {
                    profileImagePreview.src = data.imageUrl;
                }
            } else {
                alert('이미지 업로드 실패: ' + (data.error || '알 수 없는 오류'));
                // 실패 시 원래 이미지로 복원
                const originalSrc = profileImagePreview.getAttribute('data-original-src');
                if (originalSrc) {
                    profileImagePreview.src = originalSrc;
                }
            }
        })
        .catch(error => {
            console.error('이미지 업로드 오류:', error);
            alert('이미지 업로드 중 오류가 발생했습니다.');
        })
        .finally(() => {
            btnUploadImage.disabled = false;
            btnUploadImage.textContent = '이미지 선택';
            event.target.value = ''; // 파일 선택 초기화
        });
    }
    
    /**
     * 프로필 이미지 제거
     */
    function removeProfileImage() {
        if (!confirm('프로필 이미지를 제거하시겠습니까?')) {
            return;
        }
        
        fetch('/mypage/settings/remove-profile-image', {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('프로필 이미지가 제거되었습니다.');
                profileImagePreview.src = '/images/logo_icon.png';
                btnRemoveImage.style.display = 'none';
                if (profileImageInput) {
                    profileImageInput.value = '';
                }
            } else {
                alert('이미지 제거 실패: ' + (data.error || '알 수 없는 오류'));
            }
        })
        .catch(error => {
            console.error('이미지 제거 오류:', error);
            alert('이미지 제거 중 오류가 발생했습니다.');
        });
    }

    // 각 필드의 수정 버튼 클릭 이벤트
    editButtons.forEach(button => {
        button.addEventListener('click', function() {
            const fieldName = this.getAttribute('data-field');
            const input = document.getElementById(fieldName);
            
            if (input) {
                // 입력 필드 활성화
                input.disabled = false;
                input.focus();
                
                // 비밀번호 필드인 경우 타입 변경
                if (fieldName === 'password') {
                    input.type = 'password';
                }
            }
        });
    });

    // 폼 제출 시 유효성 검사
    if (form) {
        form.addEventListener('submit', function(e) {
            const currentPassword = document.getElementById('currentPassword');
            const password = document.getElementById('password');
            
            // 비밀번호 필드가 존재하는 경우에만 검증 (소셜 로그인 사용자는 필드가 없음)
            if (password && currentPassword) {
                // 새 비밀번호가 입력된 경우 현재 비밀번호 필수 확인
                if (password.value && password.value.trim() !== '') {
                    if (!currentPassword.value || currentPassword.value.trim() === '') {
                        e.preventDefault();
                        alert('비밀번호를 변경하려면 현재 비밀번호를 입력해주세요.');
                        currentPassword.focus();
                        return false;
                    }
                }
                
                // 비밀번호가 입력된 경우 유효성 검사
                if (password.value && password.value.length < 8) {
                    e.preventDefault();
                    alert('비밀번호는 최소 8자 이상이어야 합니다.');
                    password.focus();
                    return false;
                }
            }

            // 이메일 유효성 검사
            const email = document.getElementById('email');
            if (email && email.value) {
                const emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
                if (!emailRegex.test(email.value)) {
                    e.preventDefault();
                    alert('올바른 이메일 형식이 아닙니다.');
                    email.focus();
                    return false;
                }
            }

            // 전화번호 유효성 검사
            const phone = document.getElementById('phone');
            if (phone && phone.value) {
                const phoneRegex = /^[0-9-]+$/;
                if (!phoneRegex.test(phone.value)) {
                    e.preventDefault();
                    alert('올바른 전화번호 형식이 아닙니다.');
                    phone.focus();
                    return false;
                }
            }
        });
    }
    
    // 탈퇴 기능 - 이벤트 위임 사용
    console.log('[JavaScript] 탈퇴 기능 초기화 시작');
    
    // 버튼 존재 여부 확인
    setTimeout(function() {
        const btnWithdraw = document.getElementById('btn-withdraw');
        const btnConfirmWithdraw = document.getElementById('btn-confirm-withdraw');
        const withdrawModal = document.getElementById('withdraw-modal');
        const withdrawForm = document.getElementById('withdraw-form');
        
        console.log('[JavaScript] 버튼 존재 여부 확인:');
        console.log('  - btn-withdraw:', btnWithdraw ? '존재' : '없음');
        console.log('  - btn-confirm-withdraw:', btnConfirmWithdraw ? '존재' : '없음');
        console.log('  - withdraw-modal:', withdrawModal ? '존재' : '없음');
        console.log('  - withdraw-form:', withdrawForm ? '존재' : '없음');
    }, 1000);
    
    // 이벤트 위임을 사용하여 동적으로 추가된 요소에도 이벤트가 작동하도록
    document.addEventListener('click', function(e) {
        // 회원 탈퇴 버튼 클릭
        if (e.target && e.target.id === 'btn-withdraw') {
            console.log('[JavaScript] 회원 탈퇴 버튼 클릭됨');
            const withdrawModal = document.getElementById('withdraw-modal');
            if (withdrawModal) {
                withdrawModal.style.display = 'flex';
                console.log('[JavaScript] 모달 표시');
            } else {
                console.error('[JavaScript] withdraw-modal을 찾을 수 없습니다.');
            }
        }
        
        // 취소 버튼 클릭
        if (e.target && e.target.id === 'btn-cancel-withdraw') {
            console.log('[JavaScript] 취소 버튼 클릭됨');
            const withdrawModal = document.getElementById('withdraw-modal');
            if (withdrawModal) {
                withdrawModal.style.display = 'none';
                console.log('[JavaScript] 모달 닫기');
            }
        }
        
        // 탈퇴 확인 버튼 클릭
        if (e.target && e.target.id === 'btn-confirm-withdraw') {
            e.preventDefault();
            e.stopPropagation();
            
            console.log('========================================');
            console.log('[JavaScript] 탈퇴 확인 버튼 클릭됨');
            console.log('========================================');
            
            // 모달 닫기
            const withdrawModal = document.getElementById('withdraw-modal');
            if (withdrawModal) {
                console.log('[JavaScript] 모달 닫기 시작');
                withdrawModal.style.display = 'none';
                console.log('[JavaScript] 모달 닫기 완료');
            }
            
            // HTML에 있는 숨겨진 form 사용
            const withdrawForm = document.getElementById('withdraw-form');
            if (withdrawForm) {
                console.log('[JavaScript] 숨겨진 form 찾음');
                console.log('[JavaScript] Form action:', withdrawForm.action);
                console.log('[JavaScript] Form method:', withdrawForm.method);
                
                try {
                    console.log('[JavaScript] Form submit 시작');
                    withdrawForm.submit();
                    console.log('[JavaScript] Form submit 완료');
                } catch (error) {
                    console.error('[JavaScript] Form submit 중 오류:', error);
                    console.error('[JavaScript] 오류 메시지:', error.message);
                    alert('탈퇴 요청 중 오류가 발생했습니다: ' + error.message);
                }
            } else {
                console.error('[JavaScript] withdraw-form을 찾을 수 없습니다.');
                // 대안: 동적으로 form 생성
                try {
                    console.log('[JavaScript] 동적 form 생성 시작');
                    const form = document.createElement('form');
                    form.method = 'POST';
                    form.action = '/mypage/settings/withdraw';
                    form.style.display = 'none';
                    document.body.appendChild(form);
                    console.log('[JavaScript] 동적 form 생성 완료, submit 시작');
                    form.submit();
                    console.log('[JavaScript] 동적 form submit 완료');
                } catch (createError) {
                    console.error('[JavaScript] 동적 form 생성/제출 중 오류:', createError);
                    alert('탈퇴 요청 중 오류가 발생했습니다.');
                }
            }
        }
    });
    
    console.log('[JavaScript] 탈퇴 기능 이벤트 위임 등록 완료');
    
    // 모달 외부 클릭 시 닫기 (이벤트 위임 사용)
    document.addEventListener('click', function(e) {
        const withdrawModal = document.getElementById('withdraw-modal');
        if (withdrawModal && e.target === withdrawModal) {
            console.log('[JavaScript] 모달 외부 클릭 - 모달 닫기');
            withdrawModal.style.display = 'none';
        }
    });
});

