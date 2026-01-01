package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.PaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.PaymentReceiptDTO;
import com.learnit.learnit.mypage.dto.ReceiptCourseDTO;
import com.learnit.learnit.mypage.repository.MyPagePaymentMapper;
import com.learnit.learnit.payment.common.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPagePaymentService {

    private final MyPagePaymentMapper myPagePaymentMapper;

    public List<PaymentHistoryDTO> getPaymentHistories(Long userId){

        List<PaymentHistoryDTO> histories = myPagePaymentMapper.findPaymentHistories(userId);

        for(PaymentHistoryDTO dto : histories){
            List<String> titles = myPagePaymentMapper.findCourseTitlesByPaymentId(dto.getPaymentId());

            if(titles == null || titles.isEmpty()) continue;

            if(titles.size() == 1){
                dto.setCourseSummary(titles.get(0));
            }else{
                dto.setCourseSummary(titles.get(0) + " 외 " + (titles.size() - 1) + "건");
            }
        }

        return histories;
    }

    public PaymentReceiptDTO getReceipt(Long paymentId, Long userId){
        PaymentReceiptDTO receipt = myPagePaymentMapper.findPaymentReceipt(paymentId, userId);

        if(receipt == null) throw new PaymentException("영수증 정보가 없습니다.");

        List<ReceiptCourseDTO> courses = myPagePaymentMapper.findReceiptCourses(paymentId);

        receipt.setCourses(courses);
        return receipt;
    }

}
