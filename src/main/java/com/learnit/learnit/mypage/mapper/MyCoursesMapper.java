package com.learnit.learnit.mypage.mapper;

import com.learnit.learnit.mypage.dto.MyCourseSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyCoursesMapper {

    /**
     * 내 학습 강의 목록 조회 (페이징)
     */
    List<MyCourseSummaryDTO> selectMyCourses(@Param("userId") Long userId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /**
     * 내 학습 강의 총 개수
     */
    int countMyCourses(@Param("userId") Long userId);
}
