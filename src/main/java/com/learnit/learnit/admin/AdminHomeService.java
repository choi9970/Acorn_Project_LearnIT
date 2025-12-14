package com.learnit.learnit.admin;

import com.learnit.learnit.admin.AdminHome;
import com.learnit.learnit.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminHomeService {
    private final AdminMapper adminMapper;

    public AdminHome getDashboardData() {
        AdminHome adminHome = new AdminHome();

        adminHome.setActiveCourseCount(adminMapper.countActiveCourses());
        adminHome.setPendingReviewCount(adminMapper.countPendingReviews());
        adminHome.setPendingQnaCount(adminMapper.countPendingQnas());

        adminHome.setLatestNotices(adminMapper.selectLatestNotices());
        adminHome.setPendingCertificates(adminMapper.selectPendingCertificates());

        return adminHome;
    }
}
