package com.learnit.learnit.cart;

import com.learnit.learnit.auth.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ✅ 장바구니 페이지
    @GetMapping("/cart")
    public String cartPage(Model model) {
        Long userId = AuthUtil.requireLoginUserId();
        if (userId == null) return "redirect:/login";

        List<CartItem> items = cartService.getCartItems(userId);
        int totalPrice = cartService.calcTotal(items);

        model.addAttribute("items", items);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("discountPrice", 0);
        model.addAttribute("finalPrice", totalPrice);

        return "cart/cart";
    }

    // ✅ 담기 (CourseDetail에서 쓰기 좋음)
    @PostMapping("/cart/add")
    @ResponseBody
    public String addToCart(@RequestParam("courseId") Long courseId) {
        Long userId = AuthUtil.requireLoginUserId();
        if (userId == null) return "LOGIN_REQUIRED";

        boolean inserted = cartService.addToCart(userId, courseId);
        return inserted ? "OK" : "DUPLICATE";
    }


    // ✅ X 버튼 삭제
    @PostMapping("/cart/delete")
    public String deleteItem(@RequestParam("cartId") Long cartId) {
        Long userId = AuthUtil.requireLoginUserId();
        if (userId == null) return "redirect:/login";

        cartService.deleteItem(userId, cartId);
        return "redirect:/cart";
    }

    // ✅ 전체 삭제
    @PostMapping("/cart/clear")
    public String clearCart() {
        Long userId = AuthUtil.requireLoginUserId();
        if (userId == null) return "redirect:/login";

        cartService.clearCart(userId);
        return "redirect:/cart";
    }
}
