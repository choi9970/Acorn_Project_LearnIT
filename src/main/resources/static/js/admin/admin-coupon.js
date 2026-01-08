let selectedUsers = new Map();

function toggleCouponMode(mode){
    document.getElementById('existingCouponArea').style.display = (mode === 'existing') ? 'block' : 'none';
    document.getElementById('newCouponArea').style.display = (mode === 'new') ? 'flex' : 'none';
}

function toggleUserSearch(show){
    document.getElementById('userSearchContent').style.display = show ? 'block' : 'none';
    if(!show) {
        selectedUsers.clear();
        renderIssueList();
    }
}

async function searchUsers(){
    const keyword = document.getElementById('searchKeyword').value;
    const res = await fetch(`/api/admin/users/search?keyword=${encodeURIComponent(keyword)}`);
    const users = await res.json();
    const tbody = document.getElementById('userSearchResult');

    tbody.innerHTML = users.map(user => {
        const isSelected = selectedUsers.has(user.userId);

        return `
        <tr>
            <td>${user.name}</td>
            <td>${user.email}</td>
            <td>${user.userId}</td>
            <td>
                <button type="button"
                        class="btn-add"
                        ${isSelected ? 'disabled' : ''}
                        onclick='addToIssueList(${JSON.stringify(user)})'>
                    +
                </button>
            </td>
        </tr>
        `;
    }).join('');
}

function addToIssueList(user){
    if(selectedUsers.has(user.userId)) return alert("이미 추가된 회원입니다.");
    selectedUsers.set(user.userId, user);
    renderIssueList();
}

function removeFromIssueList(userId) {
    selectedUsers.delete(userId);
    renderIssueList();
}

function renderIssueList() {
    const tbody = document.getElementById('issueTargetList');
    tbody.innerHTML = Array.from(selectedUsers.values()).map(user => `
        <tr>
            <td>${user.userId}</td>
            <td>${user.name}</td>
            <td class="td-email">${user.email}</td>
            <td>
                <button class="btn-remove-icon"
                        onclick="removeFromIssueList(${user.userId})"
                        title="리스트에서 제거">
                    ✕
                </button>
            </td>
        </tr>
    `).join('');

    searchUsers();
}

async function handleIssueCoupon() {
    const couponMode = document.querySelector('input[name="couponMode"]:checked').value;
    const targetMode = document.querySelector('input[name="grantTarget"]:checked').value;

    let payload = {
        allUser: targetMode === 'all',
        userIds: targetMode === 'specific' ? Array.from(selectedUsers.keys()) : []
    };

    if (targetMode === 'specific' && payload.userIds.length === 0) return alert("지급할 회원을 선택하세요.");

    if (couponMode === 'existing') {
        payload.couponId = document.getElementById('selectedCouponId').value;
        if (!payload.couponId) return alert("지급할 쿠폰을 선택하세요.");
    } else {
        payload.name = document.getElementById('couponName').value;
        payload.minPrice = document.getElementById('minPrice').value;
        payload.discountAmount = document.getElementById('discountAmount').value;
        payload.expireDate = document.getElementById('expireDate').value;
        payload.type = "MANUAL";
        if (!payload.name || !payload.discountAmount) return alert("새 쿠폰 정보를 입력하세요.");
    }

    const res = await fetch('/api/admin/coupons/issue', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (res.ok) {
        alert("쿠폰 지급이 완료되었습니다.");
        location.reload();
    } else {
        alert("서버 오류가 발생했습니다. 로그를 확인하세요.");
    }
}