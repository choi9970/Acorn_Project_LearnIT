package com.learnit.learnit.mypage.dashboard.repository;

import com.learnit.learnit.mypage.dashboard.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DashboardRepository {
    // 최근 학습 강의 조회
    CourseSummaryDTO selectRecentCourse(@Param("userId") Long userId);

    // 주간 학습 데이터 조회
    List<WeeklyLearningDTO.DailyLearning> selectWeeklyLearning(
            @Param("userId") Long userId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    // 최근 수료증 조회
    CertificateDTO selectLatestCertificate(@Param("userId") Long userId);

    // 캘린더 데이터 조회 (월별)
    List<CalendarSummaryDTO.CalendarDay> selectCalendarData(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    // 특정 날짜의 수강한 강의 목록 조회
    List<DailyCourseDTO> selectDailyCourses(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("day") Integer day
    );
}

