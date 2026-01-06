package com.learnit.learnit.admin.coupon;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminCouponDTO {

    private Long couponId;
    private String name;           // 쿠폰명
    private String type;           // AUTO / MANUAL
    private Integer discountAmount;
    private Integer minPrice;       // 최소 주문 금액
    private LocalDate expireDate;
    private LocalDateTime createdAt;

    //쿠폰 발급
    private List<Long> userIds;
    private boolean allUser;
}
