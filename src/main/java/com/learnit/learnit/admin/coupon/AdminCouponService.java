package com.learnit.learnit.admin.coupon;

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

        //쿠폰 생성(신규)
        if (adminCouponDTO.getCouponId() == null) {
            if(adminCouponDTO.getType() == null){
                adminCouponDTO.setType("MANUAL");
            }
            adminCouponMapper.insertCoupon(adminCouponDTO);

            if (adminCouponDTO.getCouponId() == null) {
                throw new IllegalStateException("쿠폰 생성 실패");
            }
        }

        Long couponId = adminCouponDTO.getCouponId();

        //대상 유저 조회
        List<Long> targetUserIds;

        if (adminCouponDTO.isAllUser()) {
            targetUserIds = adminCouponMapper.selectAllUserIds();
        } else {
            targetUserIds = adminCouponDTO.getUserIds();
        }

        if(targetUserIds == null || targetUserIds.isEmpty()){
            if (adminCouponDTO.isAllUser()) {
                throw new IllegalStateException("지급 가능한 회원이 없습니다.");
            } else {
                throw new IllegalStateException("발급 대상자를 선택하세요.");
            }
        }

        //발급
        for(Long userId : targetUserIds){
            if(userId == null) continue;

            int exists = adminCouponMapper.existsUserCoupon(userId, couponId);
            if(exists == 0){
                adminCouponMapper.insertUserCoupon(userId, couponId);
            }
        }

    }

}
