package com.learnit.learnit.cart;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class GuestCartService {

    private static final String KEY = "GUEST_CART_COURSE_IDS";

    /**
     * 세션에서 courseId 목록 가져오기 (없으면 빈 리스트)
     * - 타입이 Long/Integer/String 섞여 있어도 안전하게 Long으로 변환
     * - null/파싱 실패 값은 무시
     */
    public List<Long> getCourseIds(HttpSession session) {
        if (session == null) return new ArrayList<>();

        Object v = session.getAttribute(KEY);
        if (!(v instanceof List<?> list)) {
            return new ArrayList<>();
        }

        List<Long> out = new ArrayList<>();
        for (Object o : list) {
            if (o == null) continue;

            if (o instanceof Long l) out.add(l);
            else if (o instanceof Integer i) out.add(i.longValue());
            else if (o instanceof String s) {
                try { out.add(Long.parseLong(s)); } catch (Exception ignore) {}
            } else {
                // 혹시 모를 타입(예: Double 등) 방어
                try { out.add(Long.valueOf(String.valueOf(o))); } catch (Exception ignore) {}
            }
        }
        return out;
    }

    /**
     * ✅ 중복이면 false, 새로 담기면 true
     * - "최신 담긴 것이 앞" 정책 유지: add(0)
     * - 혹시 세션에 중복이 들어가 있던 상태라도 정리해서 저장
     */
    public boolean add(HttpSession session, Long courseId) {
        if (session == null || courseId == null) return false;

        List<Long> ids = getCourseIds(session);

        // 이미 있으면 순서 유지하고 반환
        if (ids.contains(courseId)) {
            session.setAttribute(KEY, ids);
            return false;
        }

        // 최신이 맨 앞으로
        ids.add(0, courseId);

        // 혹시 모를 중복/오염 제거 + 현재 순서 유지
        session.setAttribute(KEY, dedupKeepOrder(ids));
        return true;
    }

    /**
     * courseId 제거
     */
    public void remove(HttpSession session, Long courseId) {
        if (session == null || courseId == null) return;

        List<Long> ids = getCourseIds(session);
        ids.removeIf(id -> id != null && id.equals(courseId));
        session.setAttribute(KEY, ids);
    }

    /**
     * 여러 courseId 제거 (결제 완료 후 등)
     */
    public void removeMany(HttpSession session, List<Long> courseIds) {
        if (session == null || courseIds == null || courseIds.isEmpty()) return;

        // contains 반복 비용 줄이기(리스트 -> Set)
        Set<Long> removeSet = new LinkedHashSet<>();
        for (Long id : courseIds) {
            if (id != null) removeSet.add(id);
        }

        List<Long> ids = getCourseIds(session);
        ids.removeIf(removeSet::contains);
        session.setAttribute(KEY, ids);
    }

    /**
     * 장바구니 비우기
     * - 기존처럼 빈 리스트를 넣어도 되지만,
     *   세션 키 자체를 지우는 방식이 더 깔끔함
     */
    public void clear(HttpSession session) {
        if (session == null) return;
        session.removeAttribute(KEY);
        // 만약 "항상 리스트가 있어야 한다" 정책이면 아래로 바꿔도 됨:
        // session.setAttribute(KEY, new ArrayList<Long>());
    }

    // ------------------ 내부 유틸 ------------------

    /**
     * 중복 제거 + 현재 순서 유지
     */
    private List<Long> dedupKeepOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        Set<Long> set = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) set.add(id);
        }
        return new ArrayList<>(set);
    }
}
