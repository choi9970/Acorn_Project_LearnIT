package com.learnit.learnit.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminHomeService {
    private final AdminMapper adminMapper;

    public AdminHome getDashboardData() {
        AdminHome adminHome = new AdminHome();

        try {
            adminHome.setActiveCourseCount(adminMapper.countActiveCourses());
        } catch (Exception e) {
            log.warn("활성 강의 수 조회 실패: {}", e.getMessage());
            adminHome.setActiveCourseCount(0);
        }

        try {
            adminHome.setPendingReviewCount(adminMapper.countPendingReviews());
        } catch (Exception e) {
            log.warn("대기 중인 리뷰 수 조회 실패: {}", e.getMessage());
            adminHome.setPendingReviewCount(0);
        }

        try {
            adminHome.setPendingQnaCount(adminMapper.countPendingQnas());
        } catch (Exception e) {
            log.warn("대기 중인 Q&A 수 조회 실패: {}", e.getMessage());
            adminHome.setPendingQnaCount(0);
        }

        try {
            adminHome.setLatestNotices(adminMapper.selectLatestNotices());
        } catch (Exception e) {
            log.warn("최신 공지사항 조회 실패: {}", e.getMessage());
            adminHome.setLatestNotices(new ArrayList<>());
        }

        try {
            adminHome.setPendingCertificates(adminMapper.selectPendingCertificates());
        } catch (Exception e) {
            log.warn("대기 중인 인증서 조회 실패: {}", e.getMessage());
            adminHome.setPendingCertificates(new ArrayList<>());
        }

        return adminHome;
    }
}
