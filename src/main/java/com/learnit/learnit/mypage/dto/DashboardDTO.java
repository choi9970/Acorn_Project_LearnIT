package com.learnit.learnit.mypage.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    private CourseSummaryDTO recentCourse;
    private WeeklyLearningDTO weeklyLearning;
    private CertificateDTO latestCertificate;
    private CalendarSummaryDTO calendarSummary;
}

