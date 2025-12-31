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

    // ✅ 단일 삭제 (X 버튼)
    public void deleteItem(Long userId, Long cartId) {
        cartMapper.deleteByUserIdAndCartId(userId, cartId);
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

    // ✅ 중복 담기 방지 + 담기
    public boolean addToCart(Long userId, Long courseId) {
        int cnt = cartMapper.exists(userId, courseId);
        if (cnt > 0) return false;
        cartMapper.insertCart(userId, courseId);
        return true;
    }

    // ✅✅ 결제된 강의만 삭제 (여러 강의 한 번에)
    public int deletePaidCourses(Long userId, List<Long> courseIds) {
        if (userId == null) return 0;
        if (courseIds == null || courseIds.isEmpty()) return 0;
        return cartMapper.deleteByUserIdAndCourseIds(userId, courseIds);
    }
}
