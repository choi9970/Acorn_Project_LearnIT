package com.learnit.learnit.cart;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cartPage(Model model) {
        Long userId = 5L; // ✅ 임시 고정

        List<CartItem> items = cartService.getCartItems(userId);
        int totalPrice = cartService.calcTotal(items);

        model.addAttribute("items", items);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("discountPrice", 0);
        model.addAttribute("finalPrice", totalPrice);

        return "cart/cart";
    }

    // ✅ 추가: 장바구니 담기
    @PostMapping("/cart/add")
    @ResponseBody
    public String addToCart(@RequestParam("courseId") Long courseId) {
        Long userId = 5L; // ✅ 임시 고정
        boolean inserted = cartService.addToCart(userId, courseId);
        return inserted ? "OK" : "DUP"; // 프론트에서 참고용
    }

    @PostMapping("/cart/delete")
    public String deleteItem(@RequestParam("cartId") Long cartId) {
        Long userId = 5L;
        cartService.removeItem(userId, cartId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart() {
        Long userId = 5L;
        cartService.clearCart(userId);
        return "redirect:/cart";
    }
}
