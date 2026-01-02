package com.learnit.learnit.payment.common.entity;

import com.learnit.learnit.payment.common.enums.PaymentMethod;
import com.learnit.learnit.payment.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "payment_prepare")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentPrepare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prepareId;

    @Column(nullable = false, unique = true)
    private String orderNo;      // UUID 주문번호

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String paymentKey;   // 카드결제: impUid / 카카오: tid

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod payType;      // KAKAO / CARD

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;      // READY, APPROVED, CANCELED, FAIL


    @Column(nullable = false, length = 1000)
    private String courseIds;    // "1,3,5"

    @Column
    private Long couponId;



    public static PaymentPrepare ready(String orderNo, Long userId, String paymentKey, int amount, List<Long> courseIds, Long couponId, PaymentMethod payType) {
        PaymentPrepare p = new PaymentPrepare();
        p.orderNo = orderNo;
        p.userId = userId;
        p.paymentKey = paymentKey;
        p.payType = payType;
        p.amount = amount;
        p.status = PaymentStatus.READY;
        p.courseIds = courseIdsToString(courseIds);
        p.couponId = couponId;
        return p;
    }


    //상태 변경 메서드(카드 OR 카카오페이)
    public void updatePaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    public void success() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }

    public void fail() {
        this.status = PaymentStatus.FAIL;
    }


    //강의ID 파싱
    private static String courseIdsToString(List<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
