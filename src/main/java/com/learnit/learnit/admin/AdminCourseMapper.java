package com.learnit.learnit.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminCourseMapper {
    List<AdminCourse> selectCourses(@Param("offset") int offset,
                                    @Param("limit") int limit,
                                    @Param("status") String status,
                                    @Param("search") String search);

    int countCourses(@Param("status") String status, 
                     @Param("search") String search);

    void updateCourseStatus(@Param("courseId") Long courseId, @Param("status") String status);
    
    void deleteCourse(@Param("courseId") Long courseId);

    void insertCourse(AdminCourseCreateDTO course);
}
