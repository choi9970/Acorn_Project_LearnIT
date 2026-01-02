package com.learnit.learnit.payment.common.dto;

import lombok.Data;

@Data
public class PaymentDetailDTO {

    //강의별 결제 내역 저장 DTO -> 판매량 인기 강의 계산
    private Long payDetailId;
    private Long paymentId;
    private Long courseId;
    private Integer price;
}
