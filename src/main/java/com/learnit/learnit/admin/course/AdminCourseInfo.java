package com.learnit.learnit.admin.course;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class AdminCourseInfo {
    private Long courseId; // 생성된 ID 반환용
    private String title;
    private String description;
    private Long categoryId;
    private int price;
    private List<Long> instructorIds; // UI에서 여러 명 선택 가능하지만, DB 구조에 따라 처리
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    // 상시 오픈 여부 (UI 편의용, 실제 저장은 날짜 null 여부로 판단 가능)
    private boolean alwaysOpen; 
}
