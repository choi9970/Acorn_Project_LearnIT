package com.learnit.learnit.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCouponDTO {

    private Long couponId;
    private String name;           // 쿠폰명
    private String type;           // AUTO / MANUAL
    private Integer discountAmount;
    private Integer minPrice;       // 최소 주문 금액
    private LocalDateTime expireDate;
    private LocalDateTime createdAt;
}
