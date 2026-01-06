package com.learnit.learnit.mypage.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyGoalDTO {
    private Long goalId;
    private Long userId;
    private Integer classGoal;      // 하루 목표 강의 수
    private Integer timeGoal;       // 하루 목표 학습 시간 (분)
    private Integer interpreterGoal; // 하루 목표 인터프리터 실행 수
    private LocalDate startDate;    // 목표 시작일 (주 시작일)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

