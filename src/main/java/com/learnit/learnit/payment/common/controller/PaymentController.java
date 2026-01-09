package com.learnit.learnit.payment.common.controller;

import com.learnit.learnit.course.CourseDTO;
import com.learnit.learnit.course.CourseMapper;
import com.learnit.learnit.enroll.EnrollmentMapper;
import com.learnit.learnit.payment.common.LoginRequiredException;
import com.learnit.learnit.payment.common.PaymentException;
import com.learnit.learnit.payment.common.dto.OrderDTO;
import com.learnit.learnit.payment.common.dto.PaymentDTO;
import com.learnit.learnit.payment.common.repository.CouponMapper;
import com.learnit.learnit.payment.common.repository.PaymentMapper;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final CouponMapper couponMapper;
    private final PaymentMapper paymentMapper;

    //결제 페이지 이동
    @GetMapping("/payment")
    public String paymentPage(
            @RequestParam("courseIds") List<Long> courseIds,
            HttpSession session,
            Model model
    ) {
        Long userId = SessionUtils.getUserId(session);

        if(userId == null) throw new LoginRequiredException("로그인이 필요한 서비스입니다.");

        // 이미 수강한 강의 필터링
        List<Long> filteredCourseIds = courseIds.stream()
                .filter(courseId ->
                        !enrollmentMapper.existsEnrollment(userId, courseId))
                .toList();

        if (filteredCourseIds.isEmpty()) {
            throw new PaymentException("이미 수강 중인 강의만 선택되었습니다.");
        }

        // 강의 정보
        List<CourseDTO> courses = courseMapper.findCoursesByIds(filteredCourseIds);

        int totalPrice = courses.stream()
                .mapToInt(c -> c.getPrice() == null ? 0 : c.getPrice())
                .sum();

        model.addAttribute("courses", courses);
        model.addAttribute("courseIds", filteredCourseIds);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("userId", userId);
        model.addAttribute("coupons",
                couponMapper.findUsableCoupons(userId));

        return "payment/payment";
    }


    //결제 결과 페이지 이동
    @GetMapping("/payment/result")
    public String result(@RequestParam String orderNo,
                         HttpSession session,
                         Model model) {

        Long userId = SessionUtils.getUserId(session);

        if(userId == null) throw new LoginRequiredException("로그인이 필요한 서비스입니다.");

        OrderDTO order = paymentMapper.findOrderByOrderNo(orderNo, userId);
        if(order == null) throw new PaymentException("주문 정보를 찾을 수 없습니다.");

        PaymentDTO payment = paymentMapper.findPaymentByOrderId(order.getOrderId());
        if(payment == null) throw new PaymentException("결제 정보를 찾을 수 없습니다.");

        model.addAttribute("order", order);
        model.addAttribute("payment", payment);

        return "payment/paymentResult";
    }


    @GetMapping("/payment/fail")
    public String fail() {
        return "payment/paymentFail";
    }
}
