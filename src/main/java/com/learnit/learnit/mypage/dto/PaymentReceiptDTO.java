package com.learnit.learnit.mypage.dto;

import com.learnit.learnit.payment.common.enums.PaymentMethod;
import com.learnit.learnit.payment.common.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentReceiptDTO {

    private Long paymentId;
    private String orderNo;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private Integer originAmount;
    private Integer discountAmount;
    private Integer totalAmount;
    private LocalDateTime paidAt;

    private List<ReceiptCourseDTO> courses;

    public String getPaymentStatusDesc() {
        return paymentStatus != null ? paymentStatus.getDescription() : "";
    }

    public String getPaymentStatusName() {
        return paymentStatus != null ? paymentStatus.name() : "";
    }

    public String getPaymentMethodDesc() {
        return paymentMethod != null ? paymentMethod.getDescription() : "";
    }

    public String getPaymentMethodName() {
        return paymentMethod != null ? paymentMethod.name() : "";
    }

}
