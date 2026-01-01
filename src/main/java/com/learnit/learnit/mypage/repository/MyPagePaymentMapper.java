package com.learnit.learnit.mypage.repository;

import com.learnit.learnit.mypage.dto.PaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.PaymentReceiptDTO;
import com.learnit.learnit.mypage.dto.ReceiptCourseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyPagePaymentMapper {

    // 마이페이지 결제 내역 조회
    List<PaymentHistoryDTO> findPaymentHistories(@Param("userId") Long userId);

    // 강의명 요약
    List<String> findCourseTitlesByPaymentId(@Param("paymentId") Long paymentId);

    /* 영수증 상세 조회
    * 결제 정보 1건
    * 결제된 강의 목록
    * */
    PaymentReceiptDTO findPaymentReceipt(@Param("paymentId") Long paymentId,
                                         @Param("userId") Long userId);

    List<ReceiptCourseDTO> findReceiptCourses(@Param("paymentId") Long paymentId);
}
