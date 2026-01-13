package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.MyPaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.MyPaymentReceiptDTO;
import com.learnit.learnit.mypage.service.MyCouponService;
import com.learnit.learnit.mypage.service.MyPaymentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyCouponController {

    private final UserService userService;
    private final MyPaymentService paymentService;
    private final MyCouponService couponService;

    private static final int PAGE_BLOCK_SIZE = 5;

    //마이페이지 - 구매/혜택 - 결제 내역 (페이징)
    // 관리자(ADMIN): 모든 결제 내역 조회
    // 서브 어드민(SUB_ADMIN): 관리하는 강의의 결제 내역만 조회
    // 일반 사용자: 본인의 결제 내역만 조회
    @GetMapping("/mypage/purchase")
    public String paymentHistory(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "4") int size,
            HttpSession session,
            Model model){

        Long userId = SessionUtils.getUserId(session);

        if (userId == null) return "redirect:/login";

        // 사용자 권한 확인
        String userRole = (String) session.getAttribute("LOGIN_USER_ROLE");
        if (userRole == null) {
            userRole = "USER"; // 기본값
        }

        UserDTO user = userService.getUserDTOById(userId);

        // 페이징 처리 (권한에 따라 분기)
        int totalCount = paymentService.getPaymentHistoriesCount(userId, userRole);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages <= 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        // 결제 내역 조회 (권한에 따라 분기)
        List<MyPaymentHistoryDTO> histories = paymentService.getPaymentHistories(userId, userRole, page, size);

        model.addAttribute("user", user);
        model.addAttribute("payments", histories != null ? histories : java.util.List.of());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pageSize", size);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "mypage/payments/myPaymentList";
    }

    //마이페이지 - 구매/혜택 - 영수증 (JSON 반환)
    @GetMapping("/mypage/purchase/{paymentId}/receipt")
    @ResponseBody
    public MyPaymentReceiptDTO paymentReceipt(@PathVariable long paymentId,
                                            HttpSession session){

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) throw new LoginRequiredException("로그인이 필요한 서비스입니다.");

        return paymentService.getReceipt(paymentId, userId);
    }

    //마이페이지 - 구매/혜택 - 쿠폰함 (페이징)
    @GetMapping("/mypage/coupons")
    public String coupons(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "4") int size,
            HttpSession session,
            Model model){

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) return "redirect:/login";

        UserDTO user = userService.getUserDTOById(userId);

        // 페이징 처리
        int totalCount = couponService.getMyCouponsCount(userId);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages <= 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        List<UserCouponDTO> coupons = couponService.getMyCouponsPaged(userId, page, size);

        model.addAttribute("user", user);
        model.addAttribute("coupons", coupons != null ? coupons : java.util.List.of());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pageSize", size);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "mypage/payments/myCouponList";
    }
}
