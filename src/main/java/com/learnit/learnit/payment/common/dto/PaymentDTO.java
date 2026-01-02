package com.learnit.learnit.payment.common.dto;

import com.learnit.learnit.payment.common.enums.PaymentMethod;
import com.learnit.learnit.payment.common.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentDTO {

    //결제 저장 DTO
    private Long paymentId;       //조회용
    private Long orderId;

    private Integer finalPrice;
    private PaymentMethod method;        //CARD / KAKAOPAY
    private PaymentStatus status;        //SUCESS / FAIL / CANCEL

    private Long couponId;
    private LocalDateTime createdAt;
}
