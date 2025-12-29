package com.learnit.learnit.payment.card.controller;

import com.learnit.learnit.payment.card.service.PaymentCardService;
import com.learnit.learnit.payment.common.LoginRequiredException;
import com.learnit.learnit.payment.common.dto.PaymentRequestDTO;
import com.learnit.learnit.payment.common.entity.PaymentPrepare;
import com.learnit.learnit.payment.common.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

//일반 결제 전용 컨트롤러
@Controller
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentService paymentService;
    private final PaymentCardService paymentCardService;

    //카드 결제 준비
    @PostMapping("/payments/card/ready")
    @ResponseBody
    public ResponseEntity<?> ready(@RequestBody PaymentRequestDTO request, HttpSession session) {

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (userId == null) throw new LoginRequiredException("로그인이 필요한 서비스입니다.");

        List<Long> courseIds = request.getCourseIds();
        Long couponId = request.getCouponId();

        /* [주의]
        * 현재 카드 결제는 테스트 결제 환경으로 구성
        * 고정 테스트 금액(100원)으로 결제 진행
        * */
        int testAmount = 100; //테스트 실결제 금액

        PaymentPrepare prepare = paymentCardService.ready(userId, testAmount, courseIds, couponId);

        return ResponseEntity.ok(
                Map.of(
                        "orderNo", prepare.getOrderNo(),
                        "amount", testAmount
                )
        );
    }


    //카드 결제 성공
    @PostMapping("/payments/card/complete")
    @ResponseBody
    public Map<String, Object> complete(@RequestBody Map<String, String> body) {

        String impUid = body.get("impUid");
        String orderNo = body.get("merchantUid");

        //1. 포트원 승인 + 검증
        PaymentPrepare prepare = paymentCardService.approve(orderNo, impUid);

        //2. 내부 로직 처리
        paymentService.processPayment(PaymentRequestDTO.fromPrepare(prepare));

        return Map.of("result", "OK", "orderNo", prepare.getOrderNo());
    }
}
