package com.learnit.learnit.courseDetail;

import com.learnit.learnit.course.CourseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseDetailMapper {
    CourseDTO selectCourseDetail(@Param("courseId") int courseId);

    // ✅ 커리큘럼(챕터) 조회 추가
    List<ChapterDTO> selectChaptersByCourseId(@Param("courseId") int courseId);
}
