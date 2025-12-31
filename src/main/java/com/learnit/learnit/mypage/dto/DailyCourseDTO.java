package com.learnit.learnit.mypage.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class DailyCourseDTO {
    private Long courseId;
    private String courseTitle;
    private Long chapterId;
    private String chapterTitle;
    private Integer chapterOrder;
    private LocalTime studiedTime;  // 학습한 시간
    private Integer studiedMinutes;   // 학습 시간 (분)
}

