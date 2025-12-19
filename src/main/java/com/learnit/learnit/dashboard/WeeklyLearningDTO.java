package com.learnit.learnit.dashboard;

import lombok.Data;
import java.util.List;

@Data
public class WeeklyLearningDTO {
    private String weekLabel;  // "25년 12월 3주차"
    private Integer weekNumber;
    private Integer year;
    private Integer month;
    private List<DailyLearning> dailyLearnings;
    private Integer totalLectures;  // 주간 총 학습 강의 수
    private Integer totalMinutes;   // 주간 총 학습 시간 (분)
    private Integer totalNotes;      // 주간 총 노트 수

    @Data
    public static class DailyLearning {
        private String dayOfWeek;  // "월", "화", "수" 등
        private Integer day;        // 날짜 (1-31)
        private Integer lectureCount;
        private Integer studyMinutes;
        private Integer noteCount;
        private Boolean hasStudy;   // 학습 기록이 있는지
    }
}

