package com.learnit.learnit.payment.common.controller;

import com.learnit.learnit.payment.common.LoginRequiredException;
import com.learnit.learnit.payment.common.dto.PaymentRequestDTO;
import com.learnit.learnit.payment.common.service.PaymentFreeService;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PaymentFreeController {

    private final PaymentFreeService paymentFreeService;

    @PostMapping("/payments/free/complete")
    @ResponseBody
    public ResponseEntity<?> completeFreePayment(@RequestBody PaymentRequestDTO request,
                                                 HttpSession session) {

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) throw new LoginRequiredException("로그인이 필요한 서비스입니다.");


        if (request.getTotalPrice() != 0) {
            return ResponseEntity.badRequest().body("무료 강의만 처리 가능한 요청입니다.");
        }

        String orderNo = paymentFreeService.processFreeOrder(userId, request.getCourseIds(), request.getCouponId());

        return ResponseEntity.ok(Map.of("orderNo", orderNo));
    }
}