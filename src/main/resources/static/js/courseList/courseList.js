document.addEventListener('DOMContentLoaded', () => {
    const state = {
        categoryId: null,
        tab: 'all',
        sort: 'popular',
        page: 0,
        size: 12,
        loading: false,
        last: false,

        // 장바구니 상태
        cartSet: new Set(),   // courseId String
        cartLoaded: false,

        // 수강중 상태
        enrolledSet: new Set(), // courseId String
        enrolledLoaded: false
    };

    const grid = document.getElementById('courseGrid');
    const sortSelect = document.getElementById('sortSelect');

    // ✅ "수강중/장바구니" 로딩이 끝나기 전에는 절대로 렌더링하지 않게 하는 게이트
    let readyResolve;
    const readyPromise = new Promise((resolve) => { readyResolve = resolve; });

    // ---------- CSRF ----------
    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        if (token && header) return { [header]: token };
        return {};
    }

    function notifyCartUpdated() {
        document.dispatchEvent(new CustomEvent('cart:updated'));
    }

    // ---------- URL <-> state ----------
    function readStateFromUrl() {
        const p = new URLSearchParams(location.search);

        const cid = p.get('categoryId');
        state.categoryId = (cid !== null && cid !== '') ? Number(cid) : null;

        state.tab = p.get('tab') || 'all';
        state.sort = p.get('sort') || 'popular';
    }

    function syncUrl(push = true) {
        const p = new URLSearchParams();
        if (state.categoryId !== null) p.set('categoryId', state.categoryId);
        if (state.tab) p.set('tab', state.tab);
        if (state.sort) p.set('sort', state.sort);

        const newUrl = p.toString() ? `/CourseList?${p.toString()}` : `/CourseList`;
        const payload = { categoryId: state.categoryId, tab: state.tab, sort: state.sort };

        if (push) history.pushState(payload, '', newUrl);
        else history.replaceState(payload, '', newUrl);
    }

    // ---------- UI ----------
    function setActiveCategory(categoryId) {
        document.querySelectorAll('.cat-item[data-category-id]').forEach(el => {
            const cid = Number(el.dataset.categoryId);
            el.classList.toggle('is-active', cid === categoryId);
        });
    }

    function setActiveTab(tab) {
        document.querySelectorAll('.tab[data-tab]').forEach(el => {
            const active = el.dataset.tab === tab;
            el.classList.toggle('is-active', active);
            el.setAttribute('aria-selected', active ? 'true' : 'false');
        });
    }

    function applyControls() {
        setActiveCategory(state.categoryId);
        setActiveTab(state.tab);
        if (sortSelect) sortSelect.value = state.sort;
    }

    function resetPaging() {
        state.page = 0;
        state.last = false;
        if (grid) grid.innerHTML = '';
    }

    // ---------- APIs ----------
    async function safeGetJson(url) {
        const res = await fetch(url, { method: 'GET', headers: { 'Accept': 'application/json' } });
        const ct = (res.headers.get('content-type') || '').toLowerCase();

        if (!res.ok) {
            const body = await res.text().catch(() => '');
            throw new Error(`${url} HTTP ${res.status} (ct=${ct}) body=${body.slice(0, 120)}`);
        }

        if (!ct.includes('application/json')) {
            const body = await res.text().catch(() => '');
            throw new Error(`${url} NOT JSON (ct=${ct}) body=${body.slice(0, 120)}`);
        }

        return await res.json();
    }

    async function loadCartIds() {
        try {
            const ids = await safeGetJson('/cart/ids');
            const s = new Set();
            (ids || []).forEach(id => s.add(String(id)));
            state.cartSet = s;
        } catch (e) {
            console.error('[CourseList] loadCartIds failed:', e);
            state.cartSet = new Set();
        } finally {
            state.cartLoaded = true;
        }
    }

    async function loadEnrolledIds() {
        try {
            const ids = await safeGetJson('/api/enrollmentsIds');
            const s = new Set();
            (ids || []).forEach(id => s.add(String(id)));
            state.enrolledSet = s;
        } catch (e) {
            console.error('[CourseList] loadEnrolledIds failed:', e);
            state.enrolledSet = new Set();
        } finally {
            state.enrolledLoaded = true;
        }
    }

    async function cartAdd(courseId) {
        const res = await fetch('/cart/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                ...csrfHeaders()
            },
            body: new URLSearchParams({ courseId })
        });
        return (await res.text()).trim();
    }

    async function cartRemove(courseId) {
        const res = await fetch('/cart/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                ...csrfHeaders()
            },
            body: new URLSearchParams({ courseId })
        });
        return (await res.text()).trim();
    }

    // ---------- Course list fetch ----------
    async function fetchPageAndAppend() {
        await readyPromise;

        if (!grid) return;
        if (state.loading || state.last) return;

        state.loading = true;

        const p = new URLSearchParams();
        if (state.categoryId !== null) p.set('categoryId', state.categoryId);
        p.set('tab', state.tab);
        p.set('sort', state.sort);
        p.set('page', String(state.page));
        p.set('size', String(state.size));

        if (state.page === 0) {
            grid.innerHTML = `<div class="loading">로딩중...</div>`;
        }

        try {
            const res = await fetch(`/api/courses?${p.toString()}`, { headers: { 'Accept': 'application/json' } });

            const ct = (res.headers.get('content-type') || '').toLowerCase();
            if (!res.ok) {
                const text = await res.text().catch(() => '');
                console.error('API ERROR', res.status, text);
                grid.innerHTML = `<div class="error">불러오기 실패 (${res.status})</div>`;
                return;
            }

            if (!ct.includes('application/json')) {
                const text = await res.text().catch(() => '');
                console.error('NOT JSON', text);
                grid.innerHTML = `<div class="error">JSON이 아닌 응답(로그인/에러페이지 가능)</div>`;
                return;
            }

            const data = await res.json();
            const content = Array.isArray(data) ? data : (data.content ?? []);
            const isLast = Array.isArray(data) ? (content.length < state.size) : !!data.last;

            if (state.page === 0) grid.innerHTML = '';

            if (!content || content.length === 0) {
                if (state.page === 0) {
                    grid.innerHTML = `
                        <div class="course-empty">
                            <h2>표시할 강의가 없어요.</h2>
                            <p>
                              선택하신 조건에 해당하는 강의를 찾을 수 없습니다.<br>
                              다른 카테고리나 필터를 선택해보세요.
                            </p>
                            <a href="/CourseList" class="btn-home">전체 강의 보기</a>
                        </div>
                    `;
                }
                state.last = true;
                return;
            }

            grid.insertAdjacentHTML('beforeend', content.map(courseCardHtml).join(''));

            state.last = isLast;
            state.page += 1;

        } catch (e) {
            console.error(e);
            if (state.page === 0) grid.innerHTML = `<div class="error">네트워크 오류</div>`;
        } finally {
            state.loading = false;
        }
    }

    function courseCardHtml(c) {
        const price = Number(c.price ?? 0);
        const priceText = (price === 0) ? '무료' : `${price.toLocaleString()}원`;
        const thumb = c.thumbnailUrl ? c.thumbnailUrl : '';
        const courseId = String(c.courseId);

        // ✅ 수강중 여부
        const isEnrolled = state.enrolledSet.has(courseId);

        // ✅ 장바구니 활성화
        const activeClass = state.cartSet.has(courseId) ? 'is-active' : '';

        // ✅ 썸네일 우상단: 수강중이면 뱃지, 아니면 아무것도(장바구니 버튼은 meta로만)
        const actionInThumb = isEnrolled
            ? `<span class="course-badge">수강중</span>`
            : ``;

        // ✅ 가격 라인 오른쪽: 수강중이면 버튼 X, 아니면 장바구니 버튼 O
        const actionInMeta = isEnrolled
            ? ``
            : `<button class="cart-btn ${activeClass}"
                      type="button"
                      aria-label="장바구니"
                      data-course-id="${courseId}">🛒</button>`;

        return `
      <article class="course-card">
        <a class="course-link" href="/CourseDetail?courseId=${courseId}&tab=intro">
          <div class="thumb-wrap">
            ${thumb
                ? `<img class="thumb" src="${escapeHtml(thumb)}" alt="">`
                : `<div class="thumb thumb-placeholder"></div>`}
            ${actionInThumb}
          </div>

          <div class="card-body">
            <h3 class="title">${escapeHtml(c.title ?? '')}</h3>
            <p class="desc">${escapeHtml(String(c.description ?? '').slice(0, 80))}</p>

            <div class="meta">
              <span class="price">${priceText}</span>
              ${actionInMeta}
            </div>
          </div>
        </a>
      </article>`;
    }

    function escapeHtml(s) {
        return String(s)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    // ---------- Click (장바구니) ----------
    if (grid) {
        grid.addEventListener('click', async (e) => {
            const btn = e.target.closest('.cart-btn');
            if (!btn) return;

            e.preventDefault();
            e.stopPropagation();

            const courseId = btn.dataset.courseId;
            if (!courseId) return;

            // 이미 담김 → 제거
            if (btn.classList.contains('is-active')) {
                try {
                    const text = await cartRemove(courseId);
                    if (text === 'OK' || text === 'NOOP') {
                        btn.classList.remove('is-active');
                        state.cartSet.delete(String(courseId));
                        notifyCartUpdated();
                    } else {
                        alert('장바구니 제거 실패: ' + text);
                    }
                } catch (err) {
                    console.error(err);
                    alert('장바구니 제거 중 오류가 발생했습니다.');
                }
                return;
            }

            // 미담김 → 추가
            try {
                const text = await cartAdd(courseId);

                if (text === 'OK' || text === 'DUPLICATE') {
                    btn.classList.add('is-active');
                    state.cartSet.add(String(courseId));
                    notifyCartUpdated();
                    return;
                }

                if (text === 'ALREADY_ENROLLED') {
                    // ✅ 서버가 수강중이라면: 세트에 추가 후 "현재 카드"를 뱃지로 갱신하고 버튼 제거
                    state.enrolledSet.add(String(courseId));

                    const card = btn.closest('.course-card');
                    if (card) {
                        // meta 버튼 제거
                        btn.remove();

                        // thumb에 수강중 배지 없으면 추가
                        const thumbWrap = card.querySelector('.thumb-wrap');
                        if (thumbWrap && !thumbWrap.querySelector('.course-badge')) {
                            thumbWrap.insertAdjacentHTML('beforeend', '<span class="course-badge">수강중</span>');
                        }
                    }
                    return;
                }

                if (text === 'LOGIN_REQUIRED') {
                    location.href = '/login';
                    return;
                }

                alert('장바구니 담기 실패: ' + text);

            } catch (err) {
                console.error(err);
                alert('장바구니 담기 중 오류가 발생했습니다.');
            }
        });
    }

    // ---------- Events ----------
    document.querySelectorAll('.cat-item[data-category-id]').forEach(catEl => {
        catEl.addEventListener('click', (e) => {
            e.preventDefault();
            state.categoryId = Number(catEl.dataset.categoryId);
            applyControls();
            syncUrl(true);
            resetPaging();
            fetchPageAndAppend();
        });
    });

    document.querySelectorAll('.tab[data-tab]').forEach(tabEl => {
        tabEl.addEventListener('click', (e) => {
            e.preventDefault();
            state.tab = tabEl.dataset.tab;
            applyControls();
            syncUrl(true);
            resetPaging();
            fetchPageAndAppend();
        });
    });

    if (sortSelect) {
        sortSelect.addEventListener('change', () => {
            state.sort = sortSelect.value;
            syncUrl(true);
            resetPaging();
            fetchPageAndAppend();
        });
    }

    window.addEventListener('scroll', () => {
        const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 300;
        if (nearBottom) fetchPageAndAppend();
    });

    window.addEventListener('popstate', (e) => {
        const s = e.state;
        if (s) {
            state.categoryId = s.categoryId ?? null;
            state.tab = s.tab ?? 'all';
            state.sort = s.sort ?? 'popular';
        } else {
            readStateFromUrl();
        }

        applyControls();
        syncUrl(false);
        resetPaging();
        fetchPageAndAppend();
    });

    // ---------- init ----------
    (async function init() {
        readStateFromUrl();
        applyControls();
        syncUrl(false);

        await loadEnrolledIds();
        await loadCartIds();

        readyResolve();

        resetPaging();
        fetchPageAndAppend();
    })();
});
