package com.learnit.learnit.payment.common.service;

import com.learnit.learnit.payment.common.dto.PaymentRequestDTO;
import com.learnit.learnit.payment.common.entity.PaymentPrepare;
import com.learnit.learnit.payment.common.enums.PaymentMethod;
import com.learnit.learnit.payment.common.repository.PaymentPrepareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFreeService {

    private final PaymentPrepareRepository paymentPrepareRepository;
    private final PaymentService paymentService;

    @Transactional
    public String processFreeOrder(Long userId, List<Long> courseIds, Long couponId){

        String orderNo = "F_" + UUID.randomUUID();

        log.info("[FREE PAYMENT] userId: {}, orderNo: {}, courses: {}", userId, orderNo, courseIds);

        PaymentPrepare prepare = PaymentPrepare.ready(
                orderNo,
                userId,
                "FREE_PASS",   // 아직 impUid 없음
                0,
                courseIds,
                couponId,
                PaymentMethod.FREE
        );

        prepare.success();
        paymentPrepareRepository.save(prepare);
        paymentService.processPayment(PaymentRequestDTO.fromPrepare(prepare));

        return orderNo;
    }
}
