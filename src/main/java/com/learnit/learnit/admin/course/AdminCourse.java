package com.learnit.learnit.admin.course;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCourse {
    private Long courseId;
    private String title;
    private String instructorName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
