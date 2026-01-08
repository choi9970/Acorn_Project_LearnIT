document.addEventListener("DOMContentLoaded", () => {
  const mapEl = document.getElementById("kakaoMap");
  const hintEl = document.getElementById("kakaoMapHint");
  const copyBtn = document.getElementById("btnCopyAddr");

  if (!mapEl) return;

  const name = mapEl.getAttribute("data-name") || "회사";
  const addr = mapEl.getAttribute("data-addr") || "";
  const lat = parseFloat(mapEl.getAttribute("data-lat"));
  const lng = parseFloat(mapEl.getAttribute("data-lng"));

  // 주소 복사
  copyBtn?.addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText(addr);
      copyBtn.textContent = "복사됨!";
      setTimeout(() => (copyBtn.textContent = "주소 복사"), 1200);
    } catch (e) {
      alert("주소 복사에 실패했어요. 수동으로 복사해주세요.");
    }
  });

  // Kakao SDK 키가 없거나 로드 실패한 경우
  if (!window.kakao || !window.kakao.maps) {
    if (hintEl) hintEl.textContent = "카카오 지도 API 키가 없거나 SDK 로드에 실패했습니다. (도메인/키 확인)";
    return;
  }

  // autoload=false 사용 시 kakao.maps.load 안에서 초기화
  window.kakao.maps.load(() => {
    try {
      const center = new kakao.maps.LatLng(lat, lng);

      const map = new kakao.maps.Map(mapEl, {
        center,
        level: 3
      });

      const marker = new kakao.maps.Marker({
        position: center
      });
      marker.setMap(map);

      const iwContent = `
        <div style="padding:10px 12px; font-size:13px; line-height:1.35;">
          <div style="font-weight:800; margin-bottom:4px;">${name}</div>
          <div style="color:#555;">${addr}</div>
        </div>
      `;
      const infoWindow = new kakao.maps.InfoWindow({
        content: iwContent
      });
      infoWindow.open(map, marker);

      // 마커 클릭 시 인포윈도우 토글
      kakao.maps.event.addListener(marker, "click", () => {
        infoWindow.open(map, marker);
      });

      if (hintEl) hintEl.textContent = "지도를 움직여 주변을 확인할 수 있어요.";
    } catch (e) {
      if (hintEl) hintEl.textContent = "지도 초기화 중 오류가 발생했습니다.";
    }
  });
});
