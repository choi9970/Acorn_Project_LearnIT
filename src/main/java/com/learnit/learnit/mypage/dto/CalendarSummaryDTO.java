package com.learnit.learnit.mypage.dto;

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
        private Boolean hasTodo;        // 할일 여부
        private Integer todoCount;      // 할일 개수
        private List<TodoItem> todos;   // 할일 목록
    }
    
    @Data
    public static class TodoItem {
        private Long todoId;
        private String title;
        private Boolean isCompleted;
    }
}

