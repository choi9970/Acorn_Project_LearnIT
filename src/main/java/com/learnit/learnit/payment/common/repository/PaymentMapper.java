package com.learnit.learnit.payment.common.repository;

import com.learnit.learnit.payment.common.dto.OrderDTO;
import com.learnit.learnit.payment.common.dto.PaymentDTO;
import com.learnit.learnit.payment.common.dto.PaymentDetailDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface PaymentMapper {

    //1. 주문
    void insertOrder(OrderDTO order);
    void updateOrderStatus(@Param("orderId") Long orderId, @Param("status") String status);

    //2. 결제
    void insertPayment(PaymentDTO payment);

    //3. 결제 상세
    void insertPaymentDetail(PaymentDetailDTO detail);

    //4. 주문, 결제 정보 조회
    OrderDTO findOrderByOrderNo(@Param("orderNo") String orderNo,
                                @Param("userId") Long userId);

    PaymentDTO findPaymentByOrderId(@Param("orderId") Long orderId);

}
