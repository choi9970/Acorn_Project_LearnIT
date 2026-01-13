package com.learnit.learnit.enroll;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnrollmentMapper {

    // ✅ XML의 id="countEnrollment" 와 반드시 동일해야 함
    int countEnrollment(@Param("userId") Long userId,
                        @Param("courseId") int courseId);

    //결제 시 수강 여부 확인
    boolean existsEnrollment(@Param("userId") Long userId,
                             @Param("courseId") Long courseId
    );

    //결제 성공 시 수강 권한 부여
    void insertEnrollment(@Param("userId") Long userId,
                          @Param("courseId") Long courseId
    );

    List<Long> selectActiveCourseIds(@Param("userId") Long userId);


}
