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

    //2. 특정 회원 검색
    public List<UserDTO> searchUsers(String keyword){
        return adminCouponMapper.searchUsers(keyword);
    }


    //4. 쿠폰 발급 (여러명)
    @Transactional
    public void issueCoupons(AdminCouponDTO adminCouponDTO){

        if (adminCouponDTO.getCouponId() == null) {
            adminCouponMapper.insertCoupon(adminCouponDTO);
            if (adminCouponDTO.getCouponId() == null) {
                throw new IllegalStateException("쿠폰 생성 실패");
            }
        }

        Long couponId = adminCouponDTO.getCouponId();
        List<Long> targetUserIds;

        if (adminCouponDTO.isAllUser()) {
            targetUserIds = adminCouponMapper.selectAllUserIds();
        } else {
            targetUserIds = adminCouponDTO.getUserIds();
        }

        if (targetUserIds != null && !targetUserIds.isEmpty()) {

            for (Long userId : targetUserIds) {
                if (!adminCouponMapper.existsUserCoupon(userId, couponId)) {
                    adminCouponMapper.insertUserCoupon(userId, couponId);
                }
            }
        }else {
            throw new RuntimeException("발급 대상자가 없습니다.");
        }
    }

}
