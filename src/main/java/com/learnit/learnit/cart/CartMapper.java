package com.learnit.learnit.cart;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {

    List<CartItem> findByUserId(@Param("userId") Long userId);

    // ✅ 추가: 중복 체크
    int exists(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ✅ 추가: 장바구니 담기
    int insertCart(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ✅ 전체 삭제
    int deleteAllByUserId(@Param("userId") Long userId);

    // ✅ (기존 기능 유지) 단일 삭제: userId + cartId
    int deleteByUserIdAndCartId(@Param("userId") Long userId,
                                @Param("cartId") Long cartId);

    // ✅✅ 결제된 강의만 삭제: userId + courseIds(List)
    int deleteByUserIdAndCourseIds(@Param("userId") Long userId,
                                   @Param("courseIds") List<Long> courseIds);
}
