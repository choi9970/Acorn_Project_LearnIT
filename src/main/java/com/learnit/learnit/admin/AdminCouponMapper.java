package com.learnit.learnit.admin;

import com.learnit.learnit.user.dto.UserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminCouponMapper {

    //1. 쿠폰 전체 목록 조회
    List<AdminCouponDTO> selectCouponList();

    //2. 새 쿠폰 저장
    void insertCoupon(AdminCouponDTO couponDTO);

    //3. 쿠폰 발급 회원 검색
    List<UserDTO> searchUsers(@Param("keyword") String keyword);

}
