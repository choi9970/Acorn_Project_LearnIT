package com.learnit.learnit.mypage.service;

import com.learnit.learnit.payment.common.dto.UserCouponDTO;
import com.learnit.learnit.payment.common.repository.CouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCouponService {

    private final CouponMapper couponMapper;

    //마이페이지 쿠폰함 (비페이징)
    public List<UserCouponDTO> getMyCoupons(Long userId) {
        return couponMapper.findMyCoupons(userId);
    }

    //마이페이지 쿠폰함 (페이징)
    public List<UserCouponDTO> getMyCouponsPaged(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return couponMapper.findMyCouponsPaged(userId, offset, size);
    }

    //마이페이지 쿠폰함 총 개수
    public int getMyCouponsCount(Long userId) {
        return couponMapper.countMyCoupons(userId);
    }
}
