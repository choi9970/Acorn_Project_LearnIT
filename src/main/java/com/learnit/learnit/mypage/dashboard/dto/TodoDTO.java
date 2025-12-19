package com.learnit.learnit.mypage.dashboard.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TodoDTO {
    private Long todoId;
    private Long userId;
    private String title;
    private String description;
    private LocalDate targetDate;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

