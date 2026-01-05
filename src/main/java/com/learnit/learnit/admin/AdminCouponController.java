package com.learnit.learnit.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    //관리자 - 유저 관리 페이지 이동 (쿠폰 구현 확인용 - 삭제 필)
    @GetMapping("/admin/user")
    public String adminUserPage() {
        return "admin/admin-coupon";
    }

    //관리자 - 쿠폰 관리 페이지 이동
    @GetMapping("/admin/coupon")
    public String adminCouponPage() {
        return "admin/admin-coupon";
    }

    //쿠폰 목록 조회
    @GetMapping("/api/admin/coupons")
    @ResponseBody
    public List<AdminCouponDTO> list(){
        return adminCouponService.getCouponList();
    }

    //새 쿠폰 생성
    @PostMapping("/api/admin/coupons")
    @ResponseBody
    public String create(@RequestBody AdminCouponDTO dto){
        System.out.println("데이터 확인: " + dto.toString());
        adminCouponService.createCoupon(dto);
        return "success";
    }
}
