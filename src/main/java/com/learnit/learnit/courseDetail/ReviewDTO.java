package com.learnit.learnit.courseDetail;

import lombok.Data;

/**
 * 강의 상세 페이지용 리뷰 DTO
 */
@Data
public class ReviewDTO {
    private String name;      // 사용자명
    private Integer rating;   // 평점
    private String comment;   // 리뷰 내용
}

