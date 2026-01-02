package com.learnit.learnit.payment.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderDTO {

    //주문 DB 저장용 DTO
    private Long orderId;    //내부 pk
    private String orderNo;  //주문번호(카카오/pg 연동)

    private Long userId;
    private Integer totalPrice;
    private String status;       //READY, PAID, CANCEL, FAIL
    private LocalDateTime createdAt;
}
