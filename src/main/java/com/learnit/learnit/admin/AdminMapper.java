package com.learnit.learnit.admin;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminMapper {
    int countActiveCourses();
    int countPendingQnas();
    int countPendingReviews();

    List<AdminHome.AdminNotice> selectLatestNotices();
    List<AdminHome.AdminCertificate> selectPendingCertificates();
}
