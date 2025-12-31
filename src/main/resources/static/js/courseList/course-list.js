document.addEventListener('DOMContentLoaded', () => {
    const state = {
        categoryId: null,
        tab: 'all',
        sort: 'popular',
        page: 0,
        size: 12,
        loading: false,
        last: false,
    };

    const grid = document.getElementById('courseGrid');
    const sortSelect = document.getElementById('sortSelect');

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
        grid.innerHTML = '';
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

        return `
      <article class="course-card">
        <a class="course-link" href="/CourseDetail?courseId=${c.courseId}&tab=intro">
          <div class="thumb-wrap">
            ${thumb
            ? `<img class="thumb" src="${escapeHtml(thumb)}" alt="">`
            : `<div class="thumb thumb-placeholder"></div>`}
            <button class="cart-btn" type="button" aria-label="장바구니">🛒</button>
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
    readStateFromUrl();
    applyControls();
    syncUrl(false);
    resetPaging();
    fetchPageAndAppend();
});
