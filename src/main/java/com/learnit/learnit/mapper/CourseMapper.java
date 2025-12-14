package com.learnit.learnit.mapper;

import com.learnit.learnit.course.Course;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CourseMapper {
    List<Course> searchCourses(String keyword);
}
