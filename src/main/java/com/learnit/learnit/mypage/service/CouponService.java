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
public class CouponService {

    private final CouponMapper couponMapper;

    //마이페이지 쿠폰함
    public List<UserCouponDTO> getMyCoupons(Long userId) {
        return couponMapper.findMyCoupons(userId);
    }
}
