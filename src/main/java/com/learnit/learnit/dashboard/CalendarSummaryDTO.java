package com.learnit.learnit.dashboard;

import lombok.Data;
import java.util.List;

@Data
public class CalendarSummaryDTO {
    private Integer year;
    private Integer month;
    private List<CalendarDay> days;

    @Data
    public static class CalendarDay {
        private Integer day;
        private Integer lectureCount;
        private Integer studyMinutes;
        private Boolean hasAttendance;  // 출석 여부
        private Boolean hasStudy;       // 학습 기록 여부
    }
}

