package com.learnit.learnit.enroll;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollment")
@Getter
@NoArgsConstructor
public class EnrollmentDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "status", length = 20)
    private String status;   // DEFAULT 'ACTIVE'

    @Column(name = "progress_rate")
    private Integer progressRate;  // TINYINT → Integer로 매핑

    @Column(name = "quiz_score", length = 50)
    private String quizScore;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}