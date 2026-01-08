package com.learnit.learnit.payment.common.dto;

import com.learnit.learnit.payment.common.PaymentException;
import com.learnit.learnit.payment.common.entity.PaymentPrepare;
import com.learnit.learnit.payment.common.enums.PaymentMethod;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
public class PaymentRequestDTO {

    //결제 요청 DTO
    private Long orderId;
    private String orderNo;

    private Long userId;
    private Integer totalPrice;

    private PaymentMethod method;          //enum -> CARD or KAKAOPAY

    private List<Long> courseIds;   //여러개의 강의
    private Long couponId;

    //공통 변환 메서드
    public static PaymentRequestDTO fromPrepare(PaymentPrepare prepare){

        PaymentRequestDTO dto = new PaymentRequestDTO();

        dto.setOrderNo(prepare.getOrderNo());
        dto.setUserId(prepare.getUserId());
        dto.setTotalPrice(prepare.getAmount());

        //강의 id null처리
        if(prepare.getCourseIds() != null){
            List<Long> courseIds = Arrays.stream(
                            prepare.getCourseIds().split(","))
                    .map(Long::valueOf)
                    .toList();

            dto.setCourseIds(courseIds);
        }

        dto.setCouponId(prepare.getCouponId());

        //결제 수단 enum으로 비교
        if(prepare.getPayType() == PaymentMethod.KAKAOPAY){
            dto.setMethod(PaymentMethod.KAKAOPAY);
        }else if(prepare.getPayType() == PaymentMethod.CARD){
            dto.setMethod(PaymentMethod.CARD);
        }else if(prepare.getPayType() == PaymentMethod.FREE){
            dto.setMethod(PaymentMethod.FREE);
        }else{
            throw new PaymentException("지원하지 않는 결제 수단입니다.");
        }

        return dto;
    }
}
