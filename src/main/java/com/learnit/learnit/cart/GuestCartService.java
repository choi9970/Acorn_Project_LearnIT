package com.learnit.learnit.cart;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GuestCartService {

    private static final String KEY = "GUEST_CART_COURSE_IDS";

    @SuppressWarnings("unchecked")
    public List<Long> getCourseIds(HttpSession session) {
        if (session == null) return new ArrayList<>();
        Object v = session.getAttribute(KEY);
        if (v instanceof List<?> list) {
            List<Long> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Long l) out.add(l);
                else if (o instanceof Integer i) out.add(i.longValue());
                else if (o instanceof String s) {
                    try { out.add(Long.parseLong(s)); } catch (Exception ignore) {}
                }
            }
            return out;
        }
        return new ArrayList<>();
    }

    /** ✅ 중복이면 false, 새로 담기면 true (최신 담긴 것이 앞에 오도록 add(0)) */
    public boolean add(HttpSession session, Long courseId) {
        if (session == null || courseId == null) return false;
        List<Long> ids = getCourseIds(session);
        if (ids.contains(courseId)) {
            session.setAttribute(KEY, ids);
            return false;
        }
        ids.add(0, courseId);
        session.setAttribute(KEY, ids);
        return true;
    }

    public void remove(HttpSession session, Long courseId) {
        if (session == null || courseId == null) return;
        List<Long> ids = getCourseIds(session);
        ids.removeIf(id -> id.equals(courseId));
        session.setAttribute(KEY, ids);
    }

    public void removeMany(HttpSession session, List<Long> courseIds) {
        if (session == null || courseIds == null || courseIds.isEmpty()) return;
        List<Long> ids = getCourseIds(session);
        ids.removeIf(courseIds::contains);
        session.setAttribute(KEY, ids);
    }

    public void clear(HttpSession session) {
        if (session == null) return;
        session.setAttribute(KEY, new ArrayList<Long>());
    }
}
