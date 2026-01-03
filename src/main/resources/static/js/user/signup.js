document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('.login-form');
    const passwordInput = document.getElementById('password');
    const passwordConfirmInput = document.getElementById('passwordConfirm');
    const emailInput = document.getElementById('email');
    const nameInput = document.getElementById('name');
    const verifyButton = document.querySelector('.btn-verify');
    let isEmailVerified = false;
    
    // 이메일 인증하기 버튼 클릭 이벤트
    if (verifyButton) {
        verifyButton.addEventListener('click', function() {
        const email = emailInput.value.trim();
        
        // 이메일 형식 검증
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email) {
            alert('이메일을 입력해주세요.');
            emailInput.focus();
            return;
        }
        
        if (!emailPattern.test(email)) {
            alert('올바른 이메일 형식을 입력해주세요.');
            emailInput.focus();
            return;
        }
        
        // 버튼 비활성화 및 로딩 표시
        verifyButton.disabled = true;
        verifyButton.textContent = '확인 중...';
        
        // AJAX로 이메일 중복 체크
        fetch('/api/user/check-email?email=' + encodeURIComponent(email))
            .then(response => response.json())
            .then(data => {
                if (data.available) {
                    alert(data.message);
                    isEmailVerified = true;
                    emailInput.style.borderColor = '#10b981';
                    verifyButton.textContent = '인증 완료';
                    verifyButton.style.backgroundColor = '#10b981';
                    verifyButton.style.color = '#fff';
                    verifyButton.disabled = true;
                } else {
                    alert(data.message);
                    isEmailVerified = false;
                    emailInput.style.borderColor = '#ef4444';
                    verifyButton.disabled = false;
                    verifyButton.textContent = '인증하기';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('이메일 확인 중 오류가 발생했습니다. 다시 시도해주세요.');
                verifyButton.disabled = false;
                verifyButton.textContent = '인증하기';
            });
        });
        
        // 이메일 입력 시 인증 상태 초기화
        emailInput.addEventListener('input', function() {
            if (isEmailVerified) {
                isEmailVerified = false;
                emailInput.style.borderColor = '';
                verifyButton.textContent = '인증하기';
                verifyButton.style.backgroundColor = '';
                verifyButton.style.color = '';
                verifyButton.disabled = false;
            }
        });
    }
    
    // 실시간 비밀번호 일치 확인
    passwordConfirmInput.addEventListener('input', function() {
        validatePasswordMatch();
    });
    
    passwordInput.addEventListener('input', function() {
        validatePasswordMatch();
    });
    
    function validatePasswordMatch() {
        const password = passwordInput.value;
        const passwordConfirm = passwordConfirmInput.value;
        
        if (passwordConfirm && password !== passwordConfirm) {
            passwordConfirmInput.setCustomValidity('비밀번호가 일치하지 않습니다.');
            passwordConfirmInput.style.borderColor = '#ef4444';
        } else {
            passwordConfirmInput.setCustomValidity('');
            passwordConfirmInput.style.borderColor = '';
        }
    }
    
    // 이메일 형식 검증
    emailInput.addEventListener('input', function() {
        const email = emailInput.value;
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        
        if (email && !emailPattern.test(email)) {
            emailInput.setCustomValidity('올바른 이메일 형식을 입력해주세요.');
            emailInput.style.borderColor = '#ef4444';
        } else {
            emailInput.setCustomValidity('');
            emailInput.style.borderColor = '';
        }
    });
    
    // 폼 제출 전 최종 검증
    form.addEventListener('submit', function(e) {
        const password = passwordInput.value;
        const passwordConfirm = passwordConfirmInput.value;
        const email = emailInput.value;
        const name = nameInput.value.trim();
        
        // 이메일 인증 확인
        if (verifyButton && !isEmailVerified) {
            e.preventDefault();
            alert('이메일 인증을 완료해주세요.');
            verifyButton.focus();
            return false;
        }
        
        // 비밀번호 일치 검증
        if (!passwordConfirm || password !== passwordConfirm) {
            e.preventDefault();
            alert('비밀번호가 일치하지 않습니다.');
            passwordConfirmInput.focus();
            return false;
        }
        
        // 이메일 형식 검증
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(email)) {
            e.preventDefault();
            alert('올바른 이메일 형식을 입력해주세요.');
            emailInput.focus();
            return false;
        }
        
        // 이름 검증
        if (!name || name.length < 2) {
            e.preventDefault();
            alert('이름을 2자 이상 입력해주세요.');
            nameInput.focus();
            return false;
        }
        
        // 비밀번호 길이 검증
        if (password.length < 6) {
            e.preventDefault();
            alert('비밀번호는 최소 6자 이상이어야 합니다.');
            passwordInput.focus();
            return false;
        }
    });
});
