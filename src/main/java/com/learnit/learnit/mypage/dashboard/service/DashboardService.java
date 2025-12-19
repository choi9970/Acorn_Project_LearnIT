package com.learnit.learnit.mypage.dashboard.service;

import com.learnit.learnit.mypage.dashboard.dto.*;
import com.learnit.learnit.mypage.dashboard.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardDTO getDashboardData(Long userId) {
        // TODO: 실제 사용자 ID를 세션에서 가져오도록 수정 필요
        // 현재는 더미 데이터와 실제 데이터를 혼합하여 사용
        
        DashboardDTO dashboard = new DashboardDTO();

        try {
            // 최근 학습 강의
            CourseSummaryDTO recentCourse = dashboardRepository.selectRecentCourse(userId);
            if (recentCourse == null || recentCourse.getTotalLectures() == null || recentCourse.getTotalLectures() == 0) {
                recentCourse = getDummyRecentCourse();
            }
            // 진행률 재계산
            if (recentCourse != null) {
                if (recentCourse.getTotalLectures() != null && recentCourse.getTotalLectures() > 0) {
                    double progress = (recentCourse.getCurrentLecture() != null ? recentCourse.getCurrentLecture() : 0) * 100.0 / recentCourse.getTotalLectures();
                    recentCourse.setProgressRate(Math.round(progress * 100.0) / 100.0);
                } else {
                    recentCourse.setProgressRate(0.0);
                }
            }
            dashboard.setRecentCourse(recentCourse);

            // 주간 학습 데이터
            LocalDate now = LocalDate.now();
            WeeklyLearningDTO weeklyLearning = getWeeklyLearningData(userId, now.getYear(), now.getMonthValue());
            dashboard.setWeeklyLearning(weeklyLearning);

            // 최근 수료증
            CertificateDTO latestCertificate = dashboardRepository.selectLatestCertificate(userId);
            dashboard.setLatestCertificate(latestCertificate);

            // 캘린더 데이터
            CalendarSummaryDTO calendarSummary = getCalendarData(userId, now.getYear(), now.getMonthValue());
            dashboard.setCalendarSummary(calendarSummary);

        } catch (Exception e) {
            // DB 조회 실패 시 더미 데이터 반환
            dashboard = getDummyDashboard();
        }

        return dashboard;
    }

    private WeeklyLearningDTO getWeeklyLearningData(Long userId, int year, int month) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        WeeklyLearningDTO weeklyLearning = new WeeklyLearningDTO();
        weeklyLearning.setYear(year);
        weeklyLearning.setMonth(month);
        weeklyLearning.setWeekNumber((startOfWeek.getDayOfMonth() - 1) / 7 + 1);
        weeklyLearning.setWeekLabel(String.format("%d년 %d월 %d주차", year, month, weeklyLearning.getWeekNumber()));

        try {
            String startDateStr = startOfWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String endDateStr = endOfWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            List<WeeklyLearningDTO.DailyLearning> dailyLearnings = dashboardRepository.selectWeeklyLearning(
                    userId, startDateStr, endDateStr
            );
            
            if (dailyLearnings == null || dailyLearnings.isEmpty()) {
                dailyLearnings = getDummyWeeklyLearning(startOfWeek);
            }
            
            weeklyLearning.setDailyLearnings(dailyLearnings);
            
            // 주간 통계 계산
            int totalLectures = dailyLearnings.stream().mapToInt(d -> d.getLectureCount() != null ? d.getLectureCount() : 0).sum();
            int totalMinutes = dailyLearnings.stream().mapToInt(d -> d.getStudyMinutes() != null ? d.getStudyMinutes() : 0).sum();
            int totalNotes = dailyLearnings.stream().mapToInt(d -> d.getNoteCount() != null ? d.getNoteCount() : 0).sum();
            
            weeklyLearning.setTotalLectures(totalLectures);
            weeklyLearning.setTotalMinutes(totalMinutes);
            weeklyLearning.setTotalNotes(totalNotes);
            
        } catch (Exception e) {
            weeklyLearning.setDailyLearnings(getDummyWeeklyLearning(startOfWeek));
            weeklyLearning.setTotalLectures(5);
            weeklyLearning.setTotalMinutes(120);
            weeklyLearning.setTotalNotes(3);
        }

        return weeklyLearning;
    }

    private CalendarSummaryDTO getCalendarData(Long userId, int year, int month) {
        CalendarSummaryDTO calendar = new CalendarSummaryDTO();
        calendar.setYear(year);
        calendar.setMonth(month);

        try {
            List<CalendarSummaryDTO.CalendarDay> days = dashboardRepository.selectCalendarData(userId, year, month);
            if (days == null || days.isEmpty()) {
                days = getDummyCalendarDays(year, month);
            }
            calendar.setDays(days);
        } catch (Exception e) {
            calendar.setDays(getDummyCalendarDays(year, month));
        }

        return calendar;
    }

    // 더미 데이터 생성 메서드들
    private CourseSummaryDTO getDummyRecentCourse() {
        CourseSummaryDTO course = new CourseSummaryDTO();
        course.setCourseId(1L);
        course.setTitle("스프링부트 입문");
        course.setThumbnailUrl("/images/course-thumbnail.jpg");
        course.setCurrentLecture(1);
        course.setTotalLectures(10); // 0이 아닌 값으로 설정
        course.setProgressRate(10.0); // 진행률 계산
        course.setEnrollmentId(1L);
        return course;
    }

    private List<WeeklyLearningDTO.DailyLearning> getDummyWeeklyLearning(LocalDate startOfWeek) {
        List<WeeklyLearningDTO.DailyLearning> dailyLearnings = new ArrayList<>();
        String[] dayNames = {"월", "화", "수", "목", "금", "토", "일"};

        for (int i = 0; i < 7; i++) {
            WeeklyLearningDTO.DailyLearning daily = new WeeklyLearningDTO.DailyLearning();
            LocalDate date = startOfWeek.plusDays(i);
            daily.setDayOfWeek(dayNames[i]);
            daily.setDay(date.getDayOfMonth());
            
            // 화요일에만 학습 데이터가 있다고 가정
            if (i == 1) {
                daily.setLectureCount(2);
                daily.setStudyMinutes(45);
                daily.setNoteCount(1);
                daily.setHasStudy(true);
            } else {
                daily.setLectureCount(0);
                daily.setStudyMinutes(0);
                daily.setNoteCount(0);
                daily.setHasStudy(false);
            }
            
            dailyLearnings.add(daily);
        }

        return dailyLearnings;
    }

    private List<CalendarSummaryDTO.CalendarDay> getDummyCalendarDays(int year, int month) {
        List<CalendarSummaryDTO.CalendarDay> days = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            CalendarSummaryDTO.CalendarDay calendarDay = new CalendarSummaryDTO.CalendarDay();
            calendarDay.setDay(day);
            
            // 랜덤하게 일부 날짜에만 학습 데이터
            if (day % 3 == 0 || day == 5 || day == 12) {
                calendarDay.setLectureCount((int)(Math.random() * 3) + 1);
                calendarDay.setStudyMinutes((int)(Math.random() * 60) + 30);
                calendarDay.setHasAttendance(true);
                calendarDay.setHasStudy(true);
            } else {
                calendarDay.setLectureCount(0);
                calendarDay.setStudyMinutes(0);
                calendarDay.setHasAttendance(false);
                calendarDay.setHasStudy(false);
            }
            
            days.add(calendarDay);
        }

        return days;
    }

    private DashboardDTO getDummyDashboard() {
        DashboardDTO dashboard = new DashboardDTO();
        dashboard.setRecentCourse(getDummyRecentCourse());
        
        LocalDate now = LocalDate.now();
        dashboard.setWeeklyLearning(getWeeklyLearningData(1L, now.getYear(), now.getMonthValue()));
        
        CertificateDTO certificate = new CertificateDTO();
        certificate.setCertificateId(1L);
        certificate.setCourseId(1L);
        certificate.setCourseTitle("초보자를 위한 파워포인트");
        certificate.setIssuedDate(LocalDate.of(2024, 7, 30));
        dashboard.setLatestCertificate(certificate);
        
        dashboard.setCalendarSummary(getCalendarData(1L, now.getYear(), now.getMonthValue()));
        
        return dashboard;
    }
}

