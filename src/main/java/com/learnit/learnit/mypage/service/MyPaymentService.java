package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.MyPaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.MyPaymentReceiptDTO;
import com.learnit.learnit.mypage.dto.MyReceiptCourseDTO;
import com.learnit.learnit.mypage.mapper.MyQnAMapper;
import com.learnit.learnit.mypage.repository.MyPaymentMapper;
import com.learnit.learnit.payment.common.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPaymentService {

    private final MyPaymentMapper myPaymentMapper;
    private final MyQnAMapper qnAMapper;

    /**
     * 결제 내역 조회 (페이징)
     * 관리자/서브어드민인 경우 분기 처리
     */
    public List<MyPaymentHistoryDTO> getPaymentHistories(Long userId, String userRole, int page, int size){
        int offset = (page - 1) * size;
        List<MyPaymentHistoryDTO> histories;

        // 관리자(ADMIN)인 경우: 모든 결제 내역 조회
        if ("ADMIN".equals(userRole)) {
            histories = myPaymentMapper.findAdminPaymentHistories(offset, size);
        }
        // 서브 어드민(SUB_ADMIN)인 경우: 관리하는 강의의 결제 내역만 조회
        else if ("SUB_ADMIN".equals(userRole)) {
            List<Integer> managedCourseIds = qnAMapper.selectManagedCourseIds(userId);
            if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                return List.of();
            }
            histories = myPaymentMapper.findSubAdminPaymentHistories(managedCourseIds, offset, size);
        }
        // 일반 사용자인 경우: 본인의 결제 내역만 조회
        else {
            histories = myPaymentMapper.findPaymentHistories(userId, offset, size);
        }

        for(MyPaymentHistoryDTO dto : histories){
            List<String> titles = myPaymentMapper.findCourseTitlesByPaymentId(dto.getPaymentId());

            if(titles == null || titles.isEmpty()) continue;

            if(titles.size() == 1){
                dto.setCourseSummary(titles.get(0));
            }else{
                dto.setCourseSummary(titles.get(0) + " 외 " + (titles.size() - 1) + "건");
            }
        }

        return histories;
    }

    /**
     * 결제 내역 총 개수
     * 관리자/서브어드민인 경우 분기 처리
     */
    public int getPaymentHistoriesCount(Long userId, String userRole) {
        // 관리자(ADMIN)인 경우: 모든 결제 내역 개수
        if ("ADMIN".equals(userRole)) {
            return myPaymentMapper.countAdminPaymentHistories();
        }
        // 서브 어드민(SUB_ADMIN)인 경우: 관리하는 강의의 결제 내역 개수
        else if ("SUB_ADMIN".equals(userRole)) {
            List<Integer> managedCourseIds = qnAMapper.selectManagedCourseIds(userId);
            if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                return 0;
            }
            return myPaymentMapper.countSubAdminPaymentHistories(managedCourseIds);
        }
        // 일반 사용자인 경우: 본인의 결제 내역 개수
        else {
            return myPaymentMapper.countPaymentHistories(userId);
        }
    }

    public MyPaymentReceiptDTO getReceipt(Long paymentId, Long userId){
        MyPaymentReceiptDTO receipt = myPaymentMapper.findPaymentReceipt(paymentId, userId);

        if(receipt == null) throw new PaymentException("영수증 정보가 없습니다.");

        List<MyReceiptCourseDTO> courses = myPaymentMapper.findReceiptCourses(paymentId);

        receipt.setCourses(courses);
        return receipt;
    }

}
