package com.learnit.learnit.payment.common.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    READY("결제 준비"),
    PAID("결제 완료"),
    SUCCESS("결제 성공"),
    CANCELED("결제 취소"),
    FAIL("결제 실패");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }
}
