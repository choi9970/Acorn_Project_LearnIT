document.addEventListener('DOMContentLoaded', () => {
    const state = {
        keyword: (typeof serverKeyword === 'string' && serverKeyword.trim())
            ? serverKeyword.trim()
            : null,
        page: 0,
        size: 12,
        loading: false,
        last: false,
    };

    const grid = document.getElementById('courseGrid');
    const msg = document.getElementById('searchMessage');

    function showMessage(html) {
        if (!msg) return;
        msg.innerHTML = html || '';
    }

    function clearMessage() {
        showMessage('');
    }

    function readStateFromUrl() {
        const p = new URLSearchParams(location.search);
        const k = p.get('keyword') ?? p.get('q');
        state.keyword = (k && k.trim()) ? k.trim() : state.keyword;
    }

    function resetPaging() {
        state.page = 0;
        state.last = false;
        if (grid) grid.innerHTML = '';
    }

    async function fetchPageAndAppend() {
        if (!grid) return;

        if (!state.keyword) {
            grid.innerHTML = '';
            showMessage(`
        <p style="font-size:1.2rem; color:#999;">검색어가 없습니다.</p>
        <a href="/home">홈으로 돌아가기</a>
      `);
            state.last = true;
            return;
        }

        if (state.loading || state.last) return;
        state.loading = true;

        const p = new URLSearchParams();
        p.set('keyword', state.keyword);
        p.set('page', String(state.page));
        p.set('size', String(state.size));

        if (state.page === 0) {
            grid.innerHTML = `<div class="loading">로딩중...</div>`;
            clearMessage();
        }

        try {
            // ✅ 너 서버에 맞춰서 이 엔드포인트만 맞추면 됨
            const res = await fetch(`/api/search/courses?${p.toString()}`, {
                headers: { 'Accept': 'application/json' }
            });

            const ct = res.headers.get('content-type') || '';
            if (!res.ok) {
                const text = await res.text().catch(() => '');
                console.error('API ERROR', res.status, text);
                grid.innerHTML = '';
                showMessage(`<div class="error">불러오기 실패 (${res.status})</div>`);
                return;
            }

            if (!ct.includes('application/json')) {
                const text = await res.text();
                console.error('NOT JSON', text);
                grid.innerHTML = '';
                showMessage(`<div class="error">JSON이 아닌 응답(로그인/에러페이지 가능)</div>`);
                return;
            }

            const data = await res.json();

            // List 또는 Page 형태 둘 다 대응
            const content = Array.isArray(data) ? data : (data.content ?? []);
            const isLast = Array.isArray(data) ? (content.length < state.size) : !!data.last;

            if (state.page === 0) grid.innerHTML = '';

            if (!content || content.length === 0) {
                state.last = true;
                grid.innerHTML = '';
                showMessage(`
          <p style="font-size:1.2rem; color:#999;">검색 결과가 없습니다.</p>
          <a href="/home">홈으로 돌아가기</a>
        `);
                return;
            }

            clearMessage();
            grid.insertAdjacentHTML('beforeend', content.map(courseCardHtml).join(''));

            state.last = isLast;
            state.page += 1;

        } catch (e) {
            console.error(e);
            if (state.page === 0) {
                grid.innerHTML = '';
                showMessage(`<div class="error">네트워크 오류</div>`);
            }
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
        <a class="course-link" href="/CourseDetail?courseId=${c.courseId}">
          <div class="thumb-wrap">
            ${thumb
            ? `<img class="thumb" src="${escapeHtml(thumb)}" alt="">`
            : `<div class="thumb thumb-placeholder"></div>`}
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

    // 무한 스크롤
    window.addEventListener('scroll', () => {
        const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 300;
        if (nearBottom) fetchPageAndAppend();
    });

    // init
    readStateFromUrl();
    resetPaging();
    fetchPageAndAppend();
});
