package com.learnit.learnit.mypage.mapper;

import com.learnit.learnit.mypage.dto.CourseSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyCoursesMapper {
    List<CourseSummaryDTO> selectMyCourses(@Param("userId") Long userId);
}
