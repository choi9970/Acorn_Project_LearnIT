package com.learnit.learnit.cart;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CartService {

    private final CartMapper cartMapper;

    public CartService(CartMapper cartMapper) {
        this.cartMapper = cartMapper;
    }

    public List<CartItem> getCartItems(Long userId) {
        return cartMapper.findByUserId(userId);
    }

    public int removeItem(Long userId, Long cartId) {
        return cartMapper.deleteByCartId(cartId, userId);
    }

    public int clearCart(Long userId) {
        return cartMapper.deleteAllByUserId(userId);
    }

    public int calcTotal(List<CartItem> items) {
        int sum = 0;
        for (CartItem i : items) {
            if (i.getPrice() != null) sum += i.getPrice();
        }
        return sum;
    }

    // ✅ 추가: 중복 담기 방지 + 담기
    public boolean addToCart(Long userId, Long courseId) {
        int cnt = cartMapper.exists(userId, courseId);
        if (cnt > 0) return false;          // 이미 담김
        cartMapper.insertCart(userId, courseId);
        return true;                         // 새로 담김
    }
}

