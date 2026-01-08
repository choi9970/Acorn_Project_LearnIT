document.addEventListener('DOMContentLoaded', () => {
    const state = {
        categoryId: null,
        tab: 'all',
        sort: 'popular',
        page: 0,
        size: 12,
        loading: false,
        last: false,

        // ✅ 장바구니 상태(로그인/비로그인 공통)
        cartSet: new Set(),   // courseId를 String으로 저장
        cartLoaded: false
    };

    const grid = document.getElementById('courseGrid');
    const sortSelect = document.getElementById('sortSelect');

    // ---------- CSRF (프로젝트에서 CSRF 켜져있으면 필요) ----------
    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        if (token && header) return { [header]: token };
        return {};
    }

    // ---------- URL <-> state ----------
    function readStateFromUrl() {
        const p = new URLSearchParams(location.search);

        const cid = p.get('categoryId');
        state.categoryId = (cid !== null && cid !== '') ? Number(cid) : null;

        const tab = p.get('tab');
        state.tab = tab ? tab : 'all';

        const sort = p.get('sort');
        state.sort = sort ? sort : 'popular';
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

    // ---------- Cart API ----------
    async function loadCartIds() {
        try {
            const res = await fetch('/cart/ids', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });

            if (!res.ok) {
                state.cartSet = new Set();
                state.cartLoaded = true;
                return;
            }

            const ids = await res.json();
            const s = new Set();
            (ids || []).forEach(id => s.add(String(id)));
            state.cartSet = s;
            state.cartLoaded = true;
        } catch (e) {
            console.error('loadCartIds error', e);
            state.cartSet = new Set();
            state.cartLoaded = true;
        }
    }

    // ✅ CourseDetail의 /cart/add 로직 재사용(폼 인코딩)
    async function cartAdd(courseId) {
        const res = await fetch('/cart/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                ...csrfHeaders()
            },
            body: new URLSearchParams({ courseId })
        });

        const text = (await res.text()).trim();
        return text; // OK | DUPLICATE | ...
    }

    // ✅ CourseList 토글용 제거 API (/cart/remove)
    async function cartRemove(courseId) {
        const res = await fetch('/cart/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                ...csrfHeaders()
            },
            body: new URLSearchParams({ courseId })
        });

        const text = (await res.text()).trim();
        return text; // OK | NOOP | ...
    }

    // ---------- API ----------
    async function fetchPageAndAppend() {
        if (!grid) return;
        if (state.loading || state.last) return;

        state.loading = true;

        const p = new URLSearchParams();
        if (state.categoryId !== null) p.set('categoryId', state.categoryId);
        p.set('tab', state.tab);
        p.set('sort', state.sort);
        p.set('page', String(state.page));
        p.set('size', String(state.size));

        // 첫 페이지면 로딩 표시
        if (state.page === 0) {
            grid.innerHTML = `<div class="loading">로딩중...</div>`;
        }

        try {
            const res = await fetch(`/api/courses?${p.toString()}`, {
                headers: { 'Accept': 'application/json' }
            });

            // ✅ JSON이 아닐 수도 있어서 확인
            const ct = res.headers.get('content-type') || '';
            if (!res.ok) {
                const text = await res.text().catch(() => '');
                console.error('API ERROR', res.status, text);
                grid.innerHTML = `<div class="error">불러오기 실패 (${res.status})</div>`;
                return;
            }

            let data;
            if (ct.includes('application/json')) {
                data = await res.json();
            } else {
                const text = await res.text();
                console.error('NOT JSON', text);
                grid.innerHTML = `<div class="error">JSON이 아닌 응답(로그인/에러페이지 가능)</div>`;
                return;
            }

            // ✅ List or Page 모두 대응
            const content = Array.isArray(data) ? data : (data.content ?? []);
            const isLast = Array.isArray(data) ? (content.length < state.size) : !!data.last;

            if (state.page === 0) grid.innerHTML = '';
            if (!content || content.length === 0) {
                if (state.page === 0) grid.innerHTML = `
                        <div class="course-empty">
                            <h2>표시할 강의가 없어요.</h2>
                            <p>
                              선택하신 조건에 해당하는 강의를 찾을 수 없습니다.<br>
                              다른 카테고리나 필터를 선택해보세요.
                            </p>
                            <a href="/CourseList" class="btn-home">전체 강의 보기</a>
                          </div>
                        `;
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

        // ✅ 장바구니에 담긴 강의면 활성화 색
        const activeClass = state.cartSet.has(courseId) ? 'is-active' : '';

        return `
      <article class="course-card">
        <a class="course-link" href="/CourseDetail?courseId=${courseId}&tab=intro">
          <div class="thumb-wrap">
            ${thumb
            ? `<img class="thumb" src="${escapeHtml(thumb)}" alt="">`
            : `<div class="thumb thumb-placeholder"></div>`}
            <button class="cart-btn ${activeClass}"
                    type="button"
                    aria-label="장바구니"
                    data-course-id="${courseId}">🛒</button>
          </div>
          <div class="card-body">
            <h3 class="title">${escapeHtml(c.title ?? '')}</h3>
            <p class="desc">${escapeHtml(String(c.description ?? '').slice(0, 80))}</p>
            <div class="meta">
              <span class="price">${priceText}</span>
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

    // ✅✅ (추가) 🛒 클릭 이벤트 위임 (무한스크롤로 추가되는 카드도 자동 적용)
    if (grid) {
        grid.addEventListener('click', async (e) => {
            const btn = e.target.closest('.cart-btn');
            if (!btn) return;

            // 카드 링크 이동 막기(🛒만)
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
                } else if (text === 'LOGIN_REQUIRED') {
                    location.href = '/login';
                } else {
                    alert('장바구니 담기 실패: ' + text);
                }
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

    // ✅ 무한 스크롤 (바닥 근처에서 다음 페이지)
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

        // ✅ 장바구니 상태 먼저 불러와서 카드 생성 시 색 반영
        await loadCartIds();

        resetPaging();
        fetchPageAndAppend();
    })();
});
