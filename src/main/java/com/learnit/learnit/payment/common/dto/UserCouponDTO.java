package com.learnit.learnit.payment.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCouponDTO {

    //쿠폰 상태 변경용
    private Long userCouponId;
    private Long userId;
    private Long couponId;
    private String name;
    private String isUsed;      // Y / N
    private LocalDateTime usedAt;

    // 검증/계산용
    private Integer discountAmount;
    private Integer minPrice;
    private LocalDateTime expireDate;
}
