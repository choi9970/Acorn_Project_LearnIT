package com.learnit.learnit.payment.common.repository;

import com.learnit.learnit.payment.common.entity.PaymentPrepare;
import com.learnit.learnit.payment.common.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentPrepareRepository extends JpaRepository<PaymentPrepare, Long> {

    //주문번호로 조회
    Optional<PaymentPrepare> findByOrderNo(String orderNo);

    //일반카드결제만 조회
    Optional<PaymentPrepare> findByOrderNoAndPayType(String orderNo, PaymentMethod payType);
}
