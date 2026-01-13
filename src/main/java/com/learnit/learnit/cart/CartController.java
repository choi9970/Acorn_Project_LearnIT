package com.learnit.learnit.cart;

import com.learnit.learnit.enroll.EnrollmentMapper;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final GuestCartService guestCartService;
    private final EnrollmentMapper enrollmentMapper;

    // ✅ 장바구니 페이지 (로그인이든 비로그인이든 진입 가능)
    @GetMapping("/cart")
    public String cartPage(Model model, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);

        List<CartItem> items;
        boolean isLoggedIn = (userId != null);

        if (isLoggedIn) {
            items = cartService.getCartItems(userId);
        } else {
            List<Long> guestIds = guestCartService.getCourseIds(session);
            items = cartService.getGuestCartItems(guestIds);
        }

        int totalPrice = cartService.calcTotal(items);

        model.addAttribute("items", items);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("discountPrice", 0);
        model.addAttribute("finalPrice", totalPrice);
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "cart/cart";
    }

    // ✅ 담기 (비로그인도 OK / DUPLICATE로 처리)
    @PostMapping("/cart/add")
    @ResponseBody
    public String addToCart(@RequestParam("courseId") Long courseId, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);

        if (userId == null) {
            boolean inserted = guestCartService.add(session, courseId);
            return inserted ? "OK" : "DUPLICATE";
        }

        boolean inserted = cartService.addToCart(userId, courseId);
        return inserted ? "OK" : "DUPLICATE";
    }

    // ✅ X 버튼 삭제 (로그인 유저: cartId 기준)
    @PostMapping("/cart/delete")
    public String deleteItem(@RequestParam("cartId") Long cartId, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) return "redirect:/cart";

        cartService.deleteItem(userId, cartId);
        return "redirect:/cart";
    }

    // ✅ X 버튼 삭제 (비로그인: courseId 기준)
    @PostMapping("/cart/delete-course")
    public String deleteItemGuest(@RequestParam("courseId") Long courseId, HttpSession session) {
        guestCartService.remove(session, courseId);
        return "redirect:/cart";
    }

    // ✅ 전체 삭제 (로그인이면 DB, 비로그인이면 세션)
    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);

        if (userId == null) {
            guestCartService.clear(session);
        } else {
            cartService.clearCart(userId);
        }
        return "redirect:/cart";
    }

    // ✅✅ 결제된 강의만 삭제 (결제 성공 콜백/완료 페이지에서 호출)
    @PostMapping("/cart/delete-paid")
    @ResponseBody
    public String deletePaid(@RequestParam("courseIds") List<Long> courseIds, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);

        if (userId == null) {
            guestCartService.removeMany(session, courseIds);
            return "OK";
        }

        int deleted = cartService.deletePaidCourses(userId, courseIds);
        return (deleted > 0) ? "OK" : "NOOP";
    }

    // 강의 목록  카트 빼기 토글용: courseId 기준 삭제 (로그인/비로그인 공통)
    @PostMapping("/cart/remove")
    @ResponseBody
    public String removeFromCart(@RequestParam("courseId") Long courseId, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);

        if (userId == null) {
            guestCartService.remove(session, courseId);
            return "OK";
        }

        int deleted = cartService.removeFromCart(userId, courseId);
        return (deleted > 0) ? "OK" : "NOOP";
    }

    //강의 목록  카트 현재 세션(로그인/비로그인)의 장바구니 courseId 목록
    @GetMapping("/cart/ids")
    @ResponseBody
    public List<Long> cartCourseIds(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);

        if (userId == null) {
            return guestCartService.getCourseIds(session);
        }
        return cartService.getCartCourseIds(userId);
    }

    /**
     * 헤더 장바구니 뱃지(개수) 실시간 갱신용 API
     * - 로그인: DB 기반 count
     * - 비로그인: 세션 기반 count
     */
    @GetMapping("/cart/count")
    @ResponseBody
    public int cartCount(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            return guestCartService.getCourseIds(session).size();
        }
        return cartService.countByUserId(userId);
    }

    /**
     * ✅✅ 로그인 직후 UX 정리용(프론트에서 1회 호출)
     * - 로그인 상태면:
     *   1) (세션) GUEST_CART_COURSE_IDS에서도 "이미 수강중" 강의 제거
     *   2) (DB) 회원 장바구니에서도 "이미 수강중" 강의 제거
     *   3) 최종 count 반환(헤더 뱃지 즉시 동기화)
     */
    @PostMapping("/cart/cleanup-enrolled")
    @ResponseBody
    public Map<String, Object> cleanupEnrolled(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);

        Map<String, Object> res = new HashMap<>();

        // 비로그인: 그냥 현재 게스트 카트 수만 반환
        if (userId == null) {
            res.put("loggedIn", false);
            res.put("removedGuest", 0);
            res.put("removedUser", 0);
            res.put("count", guestCartService.getCourseIds(session).size());
            return res;
        }

        // 1) 수강중 강의 목록
        List<Long> enrolledIds = enrollmentMapper.selectActiveCourseIds(userId);

        // 2) (세션) 게스트 장바구니에서도 제거 → UX 플래시 방지
        int removedGuest = 0;
        if (enrolledIds != null && !enrolledIds.isEmpty()) {
            int before = guestCartService.getCourseIds(session).size();
            guestCartService.removeMany(session, enrolledIds);
            int after = guestCartService.getCourseIds(session).size();
            removedGuest = Math.max(0, before - after);
        }

        // 3) (DB) 회원 장바구니에서도 제거 → 서버 일관성 보장
        int removedUser = cartService.deleteEnrolledCoursesFromCart(userId);

        // 4) 최종 카운트
        res.put("loggedIn", true);
        res.put("removedGuest", removedGuest);
        res.put("removedUser", removedUser);
        res.put("count", cartService.countByUserId(userId));
        return res;
    }
}
