package com.learnit.learnit.cart;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CartMapper {
    List<CartItem> findByUserId(@Param("userId") Long userId);

    int deleteByCartId(@Param("cartId") Long cartId, @Param("userId") Long userId);

    int deleteAllByUserId(@Param("userId") Long userId);

    // ✅ 추가: 중복 체크
    int exists(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // ✅ 추가: 장바구니 담기
    int insertCart(@Param("userId") Long userId, @Param("courseId") Long courseId);
    int deleteByUserIdAndCartId(@Param("userId") Long userId,
                                @Param("cartId") Long cartId);

}

