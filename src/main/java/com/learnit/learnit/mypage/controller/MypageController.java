package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.CertificateDTO;
import com.learnit.learnit.mypage.dto.DashboardDTO;
import com.learnit.learnit.mypage.dto.PaymentHistoryDTO;
import com.learnit.learnit.mypage.dto.PaymentReceiptDTO;
import com.learnit.learnit.mypage.dto.GitHubAnalysisDTO;
import com.learnit.learnit.mypage.dto.ProfileDTO;
import com.learnit.learnit.mypage.dto.SkillChartDTO;
import com.learnit.learnit.mypage.service.DashboardService;
import com.learnit.learnit.mypage.service.MyPagePaymentService;
import com.learnit.learnit.mypage.service.CouponService;
import com.learnit.learnit.payment.common.LoginRequiredException;
import com.learnit.learnit.payment.common.dto.UserCouponDTO;
import com.learnit.learnit.mypage.service.GitHubAnalysisService;
import com.learnit.learnit.mypage.service.ProfileService;
import com.learnit.learnit.user.util.SessionUtils;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
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
public class MypageController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final ProfileService profileService;
    private final GitHubAnalysisService githubAnalysisService;
    private final MyPagePaymentService paymentService;
    private final CouponService couponService;


    @GetMapping("/mypage")
    public String myPage() {
        // 마이페이지 기본 라우팅: /mypage → 대시보드 호출
        return "redirect:/mypage/dashboard";
    }

    @GetMapping("/mypage/dashboard")
    public String dashboard(Model model, HttpSession session) {
        // 세션에서 사용자 ID 가져오기
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
            return "redirect:/login";
        }
        
        DashboardDTO dashboard = dashboardService.getDashboardData(userId);
        model.addAttribute("dashboard", dashboard);
        
        // 사용자 정보 조회 및 추가
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);
        
        return "mypage/dashboard/dashboard";
    }

    /**
     * 프로필 페이지 조회
     */
    @GetMapping("/mypage/profile")
    public String profile(Model model, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);

        // 프로필 정보 조회
        ProfileDTO profile = profileService.getProfile(userId);
        model.addAttribute("profile", profile);

        // 수료증 목록 조회
        java.util.List<CertificateDTO> certificates = profileService.getCertificates(userId);
        model.addAttribute("certificates", certificates);

        // 저장된 GitHub 분석 결과 조회
        GitHubAnalysisDTO savedAnalysis = githubAnalysisService.getSavedGitHubAnalysis(userId);
        
        if (savedAnalysis != null) {
            SkillChartDTO skillChart = githubAnalysisService.generateSkillChart(savedAnalysis);
            model.addAttribute("githubAnalysis", savedAnalysis);
            model.addAttribute("skillChart", skillChart);
        }

        return "mypage/profile/profile";
    }

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

