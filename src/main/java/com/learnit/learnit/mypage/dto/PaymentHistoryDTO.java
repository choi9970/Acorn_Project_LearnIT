package com.learnit.learnit.mypage.dto;

import com.learnit.learnit.payment.common.enums.PaymentMethod;
import com.learnit.learnit.payment.common.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentHistoryDTO {

    private Long paymentId;
    private String orderId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Integer originAmount;
    private Integer discountAmount;
    private Integer totalAmount;
    private LocalDateTime paidAt;
    private String courseSummary;

}
