package com.learnit.learnit.mypage.dashboard.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    private CourseSummaryDTO recentCourse;
    private WeeklyLearningDTO weeklyLearning;
    private CertificateDTO latestCertificate;
    private CalendarSummaryDTO calendarSummary;
}

