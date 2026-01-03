package com.learnit.learnit.enroll;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<EnrollmentDTO, Long> {

    // 수강 중인지 여부 체크
    //사용예시
    //boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, "ACTIVE");
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);
}