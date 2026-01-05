package com.learnit.learnit.admin;

import com.learnit.learnit.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCouponService {

    private final AdminCouponMapper adminCouponMapper;

    //1. 쿠폰 전체 목록 조회
    public List<AdminCouponDTO> getCouponList(){
        return adminCouponMapper.selectCouponList();
    }

    //2. 새 쿠폰 저장
    public void createCoupon(AdminCouponDTO couponDTO){
        adminCouponMapper.insertCoupon(couponDTO);
    }

    //3. 쿠폰 발급 회원 검색
    public List<UserDTO> searchUsers(String keyword){
        return adminCouponMapper.searchUsers(keyword);
    }
}
