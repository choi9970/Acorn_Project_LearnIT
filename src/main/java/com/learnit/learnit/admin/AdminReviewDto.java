package com.learnit.learnit.admin;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * review 테이블 매핑 DTO (review_id, course_id, user_id, rating, content, comment_status 등)
 */
@Data
public class AdminReviewDto {
    private Long reviewId;
    private Long courseId;
    private String courseTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer rating;
    private String comment;
    private String commentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

