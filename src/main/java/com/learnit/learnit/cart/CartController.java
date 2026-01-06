package com.learnit.learnit.cart;

import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final GuestCartService guestCartService;

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
        if (userId == null) return "redirect:/cart"; // 비로그인은 이 엔드포인트를 쓰지 않게 템플릿에서 분기함

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
}
