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

    // ✅ X 버튼 삭제
    @PostMapping("/cart/delete")
    public String deleteItem(@RequestParam("cartId") Long cartId) {
        Long userId = 5L; // 임시 고정
        cartService.removeItem(userId, cartId);
        return "redirect:/cart";
    }

    // ✅ 전체삭제 추가
    @PostMapping("/cart/clear")
    public String clearCart() {
        Long userId = 5L; // 임시 고정
        cartService.clearCart(userId);
        return "redirect:/cart";
    }
}
