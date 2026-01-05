let selectedUsers = new Map();

//1. 쿠폰 모드 선택 (기존 쿠폰 or 신규 쿠폰)
function toggleCouponMode(mode){
    document.getElementById('existingCouponArea').style.display = (mode === 'existing') ? 'block' : 'none';
    document.getElementById('newCouponArea').style.display = (mode === 'new') ? 'flex' : 'none';
}

//2. 유저 검색 (전원 or 특정)
function toggleUserSearch(show){
    const searchContent = document.getElementById('userSearchContent');
    const listSection = document.getElementById('issueListSection');
    searchContent.style.display = show ? 'block' : 'none';
    listSection.style.display = show ? 'block' : 'none';

    if(!show) selectedUsers.clear();   //전원 부여 시 선택 목록 초기화
}

//3. 회원 검색 api 호출
async function searchUsers(){
    const keyword = document.getElementById('searchKeyword').value;

    const res = await fetch(`/api/admin/users/search?keyword=${encodeURIComponent(keyword)}`);
    const users = await res.json();

    const tbody = document.getElementById('userSearchResult');

    tbody.innerHTML = users.map(user => `
        <tr>
            <td>${user.name}</td>
            <td>${user.email}</td>
            <td>${user.userId}</td>
            <td><button type="button" class="btn-add" onclick='addToIssueList(${JSON.stringify(user)})'>추가</button></td>
        </tr>
    `).join('');
}

//4. 리스트 추가
function addToIssueList(user){
    if(selectedUsers.has(user.userId)){
        alert("이미 추가된 회원입니다.");
        return;
    }
    selectedUsers.set(user.userId, user);
    renderIssueList();
}

// 5. 리스트 삭제
function removeFromIssueList(userId) {
    selectedUsers.delete(userId);
    renderIssueList();
}

// 6. 리스트 화면 렌더링
function renderIssueList() {
    const tbody = document.getElementById('issueTargetList');
    tbody.innerHTML = Array.from(selectedUsers.values()).map(user => `
                <tr>
                    <td>${user.userId}</td>
                    <td>${user.name}</td>
                    <td>${user.email}</td>
                    <td><button type="button" class="btn-remove" onclick="removeFromIssueList(${user.userId})">리스트에서 빼기</button></td>
                </tr>
            `).join('');
}

// 7. 최종 지급 버튼 클릭
async function handleIssueCoupon() {
    const couponMode = document.querySelector('input[name="couponMode"]:checked').value;
    const targetMode = document.querySelector('input[name="grantTarget"]:checked').value;

    let payload = {
        isAllUser: targetMode === 'all'
    };

    if (targetMode === 'specific') {
        payload.userIds = Array.from(selectedUsers.keys());
    }

    // 쿠폰 정보 추출
    if (couponMode === 'existing') {
        payload.couponId = document.getElementById('selectedCouponId').value;
        if (!payload.couponId) return alert("지급할 쿠폰을 선택해주세요.");
    } else {
        payload.name = document.getElementById('couponName').value;
        payload.discountAmount = document.getElementById('discountValue').value;
        payload.expireDate = document.getElementById('expireDate').value;
        if (!payload.name) return alert("새 쿠폰 정보를 입력해주세요.");
    }

    if (targetMode === 'specific' && payload.userIds.length === 0) {
        return alert("지급할 대상을 추가해주세요.");
    }

    // 서버 전송
    const res = await fetch('/api/admin/coupons/issue', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (res.ok) {
        alert("쿠폰 지급이 완료되었습니다.");
        location.reload();
    } else {
        alert("오류가 발생했습니다.");
    }
}