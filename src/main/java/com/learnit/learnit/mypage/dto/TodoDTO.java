package com.learnit.learnit.mypage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TodoDTO {
    private Long todoId;
    private Long userId;
    private String title;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;
    
    private Boolean isCompleted;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}

