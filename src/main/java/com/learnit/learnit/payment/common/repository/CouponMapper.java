package com.learnit.learnit.payment.common.repository;

import com.learnit.learnit.payment.common.dto.UserCouponDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponMapper {

    //쿠폰 검증
    UserCouponDTO findValidUserCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    //쿠폰 사용 상태 변경
    void useCoupon(Long userCouponId);

    //결제 페이지 유저 보유 쿠폰 목록
    List<UserCouponDTO> findUsableCoupons(@Param("userId") Long userId);

    //마이페이지 쿠폰함
    List<UserCouponDTO> findMyCoupons(@Param("userId") Long userId);

}
