package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.repository.DashboardRepository;
import com.learnit.learnit.mypage.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardDTO getDashboardData(Long userId) {
        DashboardDTO dashboard = new DashboardDTO();

        try {
            // 최근 학습 강의
            CourseSummaryDTO recentCourse = dashboardRepository.selectRecentCourse(userId);
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

            // 캘린더 데이터
            CalendarSummaryDTO calendarSummary = getCalendarData(userId, now.getYear(), now.getMonthValue());
            dashboard.setCalendarSummary(calendarSummary);

        } catch (Exception e) {
            // DB 조회 실패 시 빈 데이터 반환
            log.error("대시보드 데이터 로드 실패: userId={}", userId, e);
        }

        return dashboard;
    }

    public WeeklyLearningDTO getWeeklyLearningDataByStartDate(Long userId, int year, int month, LocalDate startOfWeek) {
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        // 주의 시작일(월요일)이 속한 월을 기준으로 표시
        // 예: 12월 30일(월) ~ 1월 5일(일) 주는 "12월 5주차"로 표시
        LocalDate displayDate = startOfWeek;
        
        WeeklyLearningDTO weeklyLearning = new WeeklyLearningDTO();
        weeklyLearning.setYear(displayDate.getYear());
        weeklyLearning.setMonth(displayDate.getMonthValue());
        
        // 주차 계산: 해당 월의 첫날부터 몇 주째인지 계산
        LocalDate firstDayOfMonth = displayDate.withDayOfMonth(1);
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue(); // 1=월요일, 7=일요일
        
        // 첫 주의 시작일 계산 (월요일 기준)
        // 첫날이 월요일이면 첫날, 그렇지 않으면 전 주 월요일
        LocalDate firstWeekStart = firstDayOfMonth.minusDays(firstDayOfWeek - 1);
        
        // startOfWeek는 이미 월요일이므로 그대로 사용
        LocalDate displayWeekStart = startOfWeek;
        
        // 첫 주 시작일부터 displayWeekStart까지의 일수 계산 후 주 수로 변환
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstWeekStart, displayWeekStart);
        int weekNumber = (int) (daysBetween / 7) + 1;
        
        // 음수나 0이면 1주차로 조정
        if (weekNumber < 1) {
            weekNumber = 1;
        }
        
        weeklyLearning.setWeekNumber(weekNumber);
        weeklyLearning.setWeekLabel(String.format("%d년 %d월 %d주차", displayDate.getYear(), displayDate.getMonthValue(), weekNumber));

        try {
            String startDateStr = startOfWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String endDateStr = endOfWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            List<WeeklyLearningDTO.DailyLearning> dailyLearnings = dashboardRepository.selectWeeklyLearning(
                    userId, startDateStr, endDateStr
            );
            
            if (dailyLearnings == null) {
                dailyLearnings = new ArrayList<>();
            }
            
            weeklyLearning.setDailyLearnings(dailyLearnings);
            
            // 주간 통계 계산
            int totalLectures = dailyLearnings.stream().mapToInt(d -> d.getLectureCount() != null ? d.getLectureCount() : 0).sum();
            int totalMinutes = dailyLearnings.stream().mapToInt(d -> d.getStudyMinutes() != null ? d.getStudyMinutes() : 0).sum();
            int totalNotes = dailyLearnings.stream().mapToInt(d -> d.getNoteCount() != null ? d.getNoteCount() : 0).sum();
            
            weeklyLearning.setTotalLectures(totalLectures);
            weeklyLearning.setTotalMinutes(totalMinutes);
            weeklyLearning.setTotalNotes(totalNotes);

            // 현재 주의 목표 조회
            DailyGoalDTO goal = getCurrentDailyGoal(userId);
            weeklyLearning.setGoal(goal);
            
        } catch (Exception e) {
            log.error("주간 학습 데이터 로드 실패: userId={}, startOfWeek={}", userId, startOfWeek, e);
            weeklyLearning.setDailyLearnings(new ArrayList<>());
            weeklyLearning.setTotalLectures(0);
            weeklyLearning.setTotalMinutes(0);
            weeklyLearning.setTotalNotes(0);
        }

        return weeklyLearning;
    }

    private WeeklyLearningDTO getWeeklyLearningData(Long userId, int year, int month) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        return getWeeklyLearningDataByStartDate(userId, year, month, startOfWeek);
    }

    public CalendarSummaryDTO getCalendarData(Long userId, int year, int month) {
        CalendarSummaryDTO calendar = new CalendarSummaryDTO();
        calendar.setYear(year);
        calendar.setMonth(month);

        try {
            List<CalendarSummaryDTO.CalendarDay> days = dashboardRepository.selectCalendarData(userId, year, month);
            log.debug("캘린더 데이터 조회 결과: userId={}, year={}, month={}, daysCount={}", userId, year, month, days != null ? days.size() : 0);
            if (days == null) {
                days = new ArrayList<>();
            }
            
            // 해당 월의 모든 할일 조회
            List<TodoDTO> todos = dashboardRepository.selectTodosByMonth(userId, year, month);
            
            // 날짜별로 할일 그룹화
            Map<Integer, List<CalendarSummaryDTO.TodoItem>> todosByDay = new HashMap<>();
            if (todos != null) {
                for (TodoDTO todo : todos) {
                    int day = todo.getTargetDate().getDayOfMonth();
                    todosByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(createTodoItem(todo));
                }
            }
            
            // 각 날짜에 할일 목록 할당
            for (CalendarSummaryDTO.CalendarDay day : days) {
                List<CalendarSummaryDTO.TodoItem> dayTodos = todosByDay.get(day.getDay());
                if (dayTodos != null && !dayTodos.isEmpty()) {
                    day.setTodos(dayTodos);
                } else {
                    day.setTodos(new ArrayList<>());
                }
            }
            
            // 디버깅: 할일이 있는 날짜 확인
            days.forEach(day -> {
                if (day.getHasTodo() != null && day.getHasTodo() && day.getTodoCount() != null && day.getTodoCount() > 0) {
                    log.debug("할일 있는 날짜: day={}, todoCount={}", day.getDay(), day.getTodoCount());
                }
            });
            calendar.setDays(days);
        } catch (Exception e) {
            log.error("캘린더 데이터 로드 실패: userId={}, year={}, month={}", userId, year, month, e);
            e.printStackTrace();
            calendar.setDays(new ArrayList<>());
        }

        return calendar;
    }
    
    private CalendarSummaryDTO.TodoItem createTodoItem(TodoDTO todo) {
        CalendarSummaryDTO.TodoItem item = new CalendarSummaryDTO.TodoItem();
        item.setTodoId(todo.getTodoId());
        item.setTitle(todo.getTitle());
        item.setIsCompleted(todo.getIsCompleted());
        return item;
    }

    /**
     * 일일 학습 목표 저장
     */
    public DailyGoalDTO saveDailyGoal(Long userId, Integer classGoal, Integer timeGoal, Integer interpreterGoal) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        // 현재 주의 시작일 계산 (월요일)
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);

        DailyGoalDTO goal = new DailyGoalDTO();
        goal.setUserId(userId);
        goal.setClassGoal(classGoal != null ? classGoal : 2);
        goal.setTimeGoal(timeGoal != null ? timeGoal : 10);
        goal.setInterpreterGoal(interpreterGoal != null ? interpreterGoal : 2);
        goal.setStartDate(startOfWeek);

        dashboardRepository.upsertDailyGoal(goal);

        return dashboardRepository.selectCurrentDailyGoal(userId, startOfWeek);
    }

    /**
     * 현재 주의 일일 학습 목표 조회
     */
    public DailyGoalDTO getCurrentDailyGoal(Long userId) {
        if (userId == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);

        return dashboardRepository.selectCurrentDailyGoal(userId, startOfWeek);
    }

    /**
     * 특정 날짜의 수강한 강의 목록 조회
     */
    public List<DailyCourseDTO> getDailyCourses(Long userId, int year, int month, int day) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        try {
            List<DailyCourseDTO> courses = dashboardRepository.selectDailyCourses(userId, year, month, day);
            log.debug("일일 강의 목록 조회 결과: userId={}, year={}, month={}, day={}, coursesCount={}", 
                    userId, year, month, day, courses != null ? courses.size() : 0);
            return courses != null ? courses : new ArrayList<>();
        } catch (Exception e) {
            log.error("일일 강의 목록 로드 실패: userId={}, year={}, month={}, day={}", userId, year, month, day, e);
            return new ArrayList<>();
        }
    }

}

