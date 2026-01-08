package com.learnit.learnit.payment.common.enums;

import lombok.Getter;

@Getter
public enum PaymentMethod {

    CARD("일반 카드결제"), KAKAOPAY("카카오페이"), FREE("무료");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

}
