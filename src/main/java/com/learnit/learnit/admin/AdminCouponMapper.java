package com.learnit.learnit.admin;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminCouponMapper {

    //1. 쿠폰 전체 목록 조회
    List<AdminCouponDTO> selectCouponList();

    //2. 새 쿠폰 저장
    void insertCoupon(AdminCouponDTO couponDTO);
}
