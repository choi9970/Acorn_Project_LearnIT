async function loadBlogPosts() {
    try {
        // RSS 주소
        const rssUrl = encodeURIComponent('https://rss.blog.naver.com/mapo_bori.xml');
        const proxyUrl = `https://corsproxy.io/?${rssUrl}`; // CORS 우회 프록시

        const res = await fetch(proxyUrl);
        const text = await res.text(); // JSON이 아니라 그냥 텍스트(XML)로 받음

        // XML 파싱
        const parser = new DOMParser();
        const xml = parser.parseFromString(text, "text/xml");

        // "LearnIT" 카테고리 글 3개
        const items = Array.from(xml.querySelectorAll("item"))
            .filter(item => item.querySelector("category")?.textContent.trim() === "LearnIT")
            .slice(0,3)
            .map(item => {
                const descHTML = item.querySelector("description")?.textContent || "";
                const imgMatch = descHTML.match(/<img.*?src="(.*?)"/i);
                const originalImg = imgMatch ? imgMatch[1] : "/img/blog-placeholder.jpg";
                const imgSrc = originalImg.startsWith("/img/")
                    ? originalImg
                    : `https://corsproxy.io/?${encodeURIComponent(originalImg)}`;
                const textDesc = descHTML.replace(/<[^>]+>/g, '').trim();
                const rawDate = item.querySelector("pubDate")?.textContent || "";
                const formattedDate = rawDate
                    ? new Date(rawDate).toLocaleDateString("ko-KR") : "";

                return {
                    title: item.querySelector("title")?.textContent || "",
                    link: item.querySelector("link")?.textContent || "#",
                    desc: textDesc,
                    date: formattedDate,
                    img: imgSrc
                };
            });

        const blogGrid = document.getElementById("blogGrid");
        blogGrid.innerHTML = ""; // 기존 내용 초기화
        items.forEach(post => {
            const card = document.createElement("div");
            card.className = "blog-card";
            card.innerHTML = `
                            <a href="${post.link}" target="_blank" style="text-decoration:none; color:inherit;">
                              <img src="${post.img}" alt="${post.title}">
                              <div class="blog-content">
                                <h3>${post.title}</h3>
                                <p>${post.desc}</p>
                                <div class="blog-date">${post.date}</div>
                              </div>
                            </a>
                          `;
            blogGrid.appendChild(card);
        });

    } catch(err) {
        console.error("블로그 글 로딩 실패:", err);
    }
}

// 페이지 로드 시 실행
document.addEventListener("DOMContentLoaded", loadBlogPosts);
