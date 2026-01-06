package com.learnit.learnit.cart;

import org.springframework.stereotype.Service;

import java.util.Collections;
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

    // ✅ 비로그인(세션) 장바구니 조회
    public List<CartItem> getGuestCartItems(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return Collections.emptyList();
        return cartMapper.findByCourseIds(courseIds);
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

    // ✅✅ 로그인 성공 시: (게스트) 장바구니를 (회원) 장바구니로 "추가 병합" (덮어쓰기 X)
    public int mergeGuestCartToUser(Long userId, List<Long> guestCourseIds) {
        if (userId == null) return 0;
        if (guestCourseIds == null || guestCourseIds.isEmpty()) return 0;

        // 중복 제거(게스트 목록 내 중복 방지)
        List<Long> distinctIds = guestCourseIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();

        int merged = 0;

        for (Long courseId : distinctIds) {
            // ✅ 회원 장바구니에 이미 있으면 스킵
            int exists = cartMapper.exists(userId, courseId);
            if (exists > 0) continue;

            // ✅ 없으면 추가
            cartMapper.insertCart(userId, courseId);
            merged++;
        }

        return merged;
    }

}
