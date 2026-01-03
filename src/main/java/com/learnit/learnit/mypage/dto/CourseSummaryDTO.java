package com.learnit.learnit.mypage.dto;

import lombok.Data;

@Data
public class CourseSummaryDTO {
    private Long courseId;
    private String title;
    private String thumbnailUrl;
    private Integer currentLecture;  // 현재 학습한 강의 번호
    private Integer totalLectures;   // 전체 강의 수
    private Double progressRate;     // 진행률 (%)
    private Long enrollmentId;
    private Long lastChapterId;      // 마지막으로 학습한 챕터 ID
}

