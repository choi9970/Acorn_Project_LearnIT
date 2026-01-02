package com.learnit.learnit.payment.common.service;

import com.learnit.learnit.payment.common.dto.PaymentRequestDTO;

public interface PaymentService {

    //승인 완료 후 검증 로직
    void processPayment(PaymentRequestDTO request);
}
