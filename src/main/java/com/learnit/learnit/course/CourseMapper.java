package com.learnit.learnit.course;

import com.learnit.learnit.home.MainCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMapper {
    //
    List<CourseDTO> searchCourses(@Param("keyword") String keyword);

    //메인 페이지
    //인기 강의 목록 불러오기
    List<MainCourse> selectPopularCourse(int limit);

    //썸네일배너 강의 불러오기
    List<MainCourse> selectBannerCourse(int limit);

    // ✅ 목록 필터/정렬(ajax용)
    // 기존(필요하면 유지)
    List<CourseDTO> selectCourses(@Param("categoryId") Integer categoryId,
                                  @Param("sort") String sort,
                                  @Param("tab") String tab);

    // ✅ 무한스크롤(페이지)용
    List<CourseDTO> selectCoursesPage(@Param("categoryId") Integer categoryId,
                                      @Param("sort") String sort,
                                      @Param("tab") String tab,
                                      @Param("limit") int limit,
                                      @Param("offset") int offset);

    // ✅ 총 개수(페이지 계산용)
    long countCourses(@Param("categoryId") Integer categoryId,
                      @Param("tab") String tab);
}
