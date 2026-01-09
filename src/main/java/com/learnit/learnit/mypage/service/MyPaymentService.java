package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.MyPaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.MyPaymentReceiptDTO;
import com.learnit.learnit.mypage.dto.MyReceiptCourseDTO;
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

    /**
     * 결제 내역 조회 (페이징)
     */
    public List<MyPaymentHistoryDTO> getPaymentHistories(Long userId, int page, int size){
        int offset = (page - 1) * size;
        List<MyPaymentHistoryDTO> histories = myPaymentMapper.findPaymentHistories(userId, offset, size);

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
     */
    public int getPaymentHistoriesCount(Long userId) {
        return myPaymentMapper.countPaymentHistories(userId);
    }

    public MyPaymentReceiptDTO getReceipt(Long paymentId, Long userId){
        MyPaymentReceiptDTO receipt = myPaymentMapper.findPaymentReceipt(paymentId, userId);

        if(receipt == null) throw new PaymentException("영수증 정보가 없습니다.");

        List<MyReceiptCourseDTO> courses = myPaymentMapper.findReceiptCourses(paymentId);

        receipt.setCourses(courses);
        return receipt;
    }

}
