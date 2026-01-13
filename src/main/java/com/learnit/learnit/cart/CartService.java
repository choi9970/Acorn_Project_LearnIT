package com.learnit.learnit.cart;

import com.learnit.learnit.enroll.EnrollmentMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CartService {

    private final CartMapper cartMapper;
    private final EnrollmentMapper enrollmentMapper;

    public CartService(CartMapper cartMapper, EnrollmentMapper enrollmentMapper) {
        this.cartMapper = cartMapper;
        this.enrollmentMapper = enrollmentMapper;
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
    // ✅ 이미 수강중이면 담기 자체를 막는다(서버 최종 보장)
    public boolean addToCart(Long userId, Long courseId) {
        if (userId != null && courseId != null) {
            boolean enrolled = enrollmentMapper.existsEnrollment(userId, courseId);
            if (enrolled) return false;
        }

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
    // ✅ 이미 수강중인 강의는 병합 대상에서 제외
    public int mergeGuestCartToUser(Long userId, List<Long> guestCourseIds) {
        if (userId == null) return 0;
        if (guestCourseIds == null || guestCourseIds.isEmpty()) return 0;

        // 중복 제거(게스트 목록 내 중복 방지)
        List<Long> distinctIds = guestCourseIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();

        // 수강중 강의 Set(빠른 contains)
        Set<Long> enrolledSet = new HashSet<>(
                enrollmentMapper.selectActiveCourseIds(userId)
        );

        int merged = 0;

        for (Long courseId : distinctIds) {
            // ✅ 이미 수강중이면 병합 제외
            if (enrolledSet.contains(courseId)) continue;

            // ✅ 회원 장바구니에 이미 있으면 스킵
            int exists = cartMapper.exists(userId, courseId);
            if (exists > 0) continue;

            // ✅ 없으면 추가
            cartMapper.insertCart(userId, courseId);
            merged++;
        }

        return merged;
    }

    /**
     * ✅ 로그인 직후(또는 필요 시점)에
     * "이미 수강중인 강의"가 회원 장바구니(DB)에 남아있지 않도록 정리
     */
    public int deleteEnrolledCoursesFromCart(Long userId) {
        if (userId == null) return 0;

        List<Long> enrolledIds = enrollmentMapper.selectActiveCourseIds(userId);
        if (enrolledIds == null || enrolledIds.isEmpty()) return 0;

        return cartMapper.deleteByUserIdAndCourseIds(userId, enrolledIds);
    }

    // 강의 목록 카트용 장바구니에 담긴 courseId 목록
    public List<Long> getCartCourseIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        return cartMapper.findCourseIdsByUserId(userId);
    }

    // 강의 목록 카트용 토글용 로그인 유저 강의 제거: userId + courseId
    public int removeFromCart(Long userId, Long courseId) {
        if (userId == null || courseId == null) return 0;
        return cartMapper.deleteByUserIdAndCourseId(userId, courseId);
    }

    // ✅ 헤더 뱃지(장바구니 개수) 조회
    public int countByUserId(Long userId) {
        if (userId == null) return 0;
        return cartMapper.countByUserId(userId);
    }
}
