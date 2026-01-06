package com.learnit.learnit.admin.coupon;

import com.learnit.learnit.payment.common.LoginRequiredException;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    //관리자 - 쿠폰 관리 페이지 이동
    @GetMapping("/admin/coupon")
    public String adminCouponPage(Model model) {
        List<AdminCouponDTO> couponList = adminCouponService.getCouponList();
        model.addAttribute("coupons", couponList);
        return "admin/admin-coupon";
    }

    //쿠폰 목록 조회
    @GetMapping("/api/admin/coupons")
    @ResponseBody
    public List<AdminCouponDTO> list(){
        return adminCouponService.getCouponList();
    }

    //회원 검색
    @GetMapping("/api/admin/users/search")
    @ResponseBody
    public List<UserDTO> search(@RequestParam(required = false) String keyword){
        return adminCouponService.searchUsers(keyword);
    }

    //쿠폰 발급
    @PostMapping("/api/admin/coupons/issue")
    @ResponseBody
    public ResponseEntity<String> issue(@RequestBody AdminCouponDTO adminCouponDTO,
                                        HttpSession session){

        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            throw new LoginRequiredException("로그인이 필요한 서비스입니다.");
        }

        String role = (String) session.getAttribute("LOGIN_USER_ROLE");
        if(!"ADMIN".equals(role)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ADMIN만 쿠폰 발급이 가능합니다.");
        }

        adminCouponService.issueCoupons(adminCouponDTO);
        return ResponseEntity.ok("success");
    }

}
