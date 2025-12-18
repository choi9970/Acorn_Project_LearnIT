package com.learnit.learnit.cart;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CartMapper {
    List<CartItem> findByUserId(@Param("userId") Long userId);

    int deleteByCartId(@Param("cartId") Long cartId, @Param("userId") Long userId);

    // ✅ 전체삭제
    int deleteAllByUserId(@Param("userId") Long userId);
}
