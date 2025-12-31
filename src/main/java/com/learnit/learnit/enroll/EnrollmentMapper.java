package com.learnit.learnit.enroll;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EnrollmentMapper {

    // ✅ XML의 id="countEnrollment" 와 반드시 동일해야 함
    int countEnrollment(@Param("userId") Long userId,
                        @Param("courseId") int courseId);
}
