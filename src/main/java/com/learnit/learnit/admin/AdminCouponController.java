package com.learnit.learnit.admin;

import com.learnit.learnit.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
        adminCouponService.createCoupon(dto);
        return "success";
    }

    //회원 검색
    @GetMapping("/api/admin/users/search")
    @ResponseBody
    public List<UserDTO> search(@RequestParam(required = false) String keyword){
        return adminCouponService.searchUsers(keyword);
    }

}
