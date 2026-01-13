package com.learnit.learnit.mypage.repository;

import com.learnit.learnit.mypage.dto.MyPaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.MyPaymentReceiptDTO;
import com.learnit.learnit.mypage.dto.MyReceiptCourseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyPaymentMapper {

    // 마이페이지 결제 내역 조회
    List<MyPaymentHistoryDTO> findPaymentHistories(@Param("userId") Long userId,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    // 마이페이지 결제 내역 총 개수
    int countPaymentHistories(@Param("userId") Long userId);

    // 관리자(ADMIN)가 볼 수 있는 모든 결제 내역 조회
    List<MyPaymentHistoryDTO> findAdminPaymentHistories(@Param("offset") int offset,
                                                        @Param("limit") int limit);

    // 관리자(ADMIN)가 볼 수 있는 모든 결제 내역 총 개수
    int countAdminPaymentHistories();

    // 서브 어드민(SUB_ADMIN)이 관리하는 강의의 결제 내역 조회
    List<MyPaymentHistoryDTO> findSubAdminPaymentHistories(@Param("managedCourseIds") List<Integer> managedCourseIds,
                                                           @Param("offset") int offset,
                                                           @Param("limit") int limit);

    // 서브 어드민(SUB_ADMIN)이 관리하는 강의의 결제 내역 총 개수
    int countSubAdminPaymentHistories(@Param("managedCourseIds") List<Integer> managedCourseIds);

    // 강의명 요약
    List<String> findCourseTitlesByPaymentId(@Param("paymentId") Long paymentId);

    /* 영수증 상세 조회
    * 결제 정보 1건
    * 결제된 강의 목록
    * */
    MyPaymentReceiptDTO findPaymentReceipt(@Param("paymentId") Long paymentId,
                                         @Param("userId") Long userId);

    List<MyReceiptCourseDTO> findReceiptCourses(@Param("paymentId") Long paymentId);
}
