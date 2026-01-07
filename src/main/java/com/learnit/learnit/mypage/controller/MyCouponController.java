package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.PaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.PaymentReceiptDTO;
import com.learnit.learnit.mypage.service.CouponService;
import com.learnit.learnit.mypage.service.MyPagePaymentService;
import com.learnit.learnit.payment.common.LoginRequiredException;
import com.learnit.learnit.payment.common.dto.UserCouponDTO;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyCouponController {

    private final UserService userService;
    private final MyPagePaymentService paymentService;
    private final CouponService couponService;

    //마이페이지 - 구매/혜택 - 결제 내역
    @GetMapping("/mypage/purchase")
    public String paymentHistory(HttpSession session, Model model){

        Long userId = SessionUtils.getUserId(session);

        if (userId == null) return "redirect:/login";

        UserDTO user = userService.getUserDTOById(userId);

        List<PaymentHistoryDTO> histories = paymentService.getPaymentHistories(userId);

        model.addAttribute("user", user);
        model.addAttribute("payments", histories);

        return "mypage/payments/payment_list";
    }

    //마이페이지 - 구매/혜택 - 영수증 (JSON 반환)
    @GetMapping("/mypage/purchase/{paymentId}/receipt")
    @ResponseBody
    public PaymentReceiptDTO paymentReceipt(@PathVariable long paymentId,
                                            HttpSession session){

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) throw new LoginRequiredException("로그인이 필요한 서비스입니다.");

        return paymentService.getReceipt(paymentId, userId);
    }

    //마이페이지 - 구매/혜택 - 쿠폰함
    @GetMapping("/mypage/coupons")
    public String coupons(HttpSession session, Model model){

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) return "redirect:/login";

        UserDTO user = userService.getUserDTOById(userId);

        List<UserCouponDTO> coupons = couponService.getMyCoupons(userId);

        model.addAttribute("user", user);
        model.addAttribute("coupons", coupons);


        return "mypage/payments/coupon_list";
    }
}
