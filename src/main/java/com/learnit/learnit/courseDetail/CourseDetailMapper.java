package com.learnit.learnit.courseDetail;

import com.learnit.learnit.course.CourseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseDetailMapper {

    CourseDTO selectCourseDetail(@Param("courseId") int courseId);

    List<ChapterDTO> selectChaptersByCourseId(@Param("courseId") int courseId);

    // ✅ 추가
    String selectInstructorNameByUserId(@Param("userId") int userId);

    String selectPeriodTextByCourseId(@Param("courseId") int courseId);

    String selectCategoryNameByCategoryId(@Param("categoryId") int categoryId);

    /**
     * 강의별 리뷰 조회 (승인된 리뷰만)
     */
    List<ReviewDTO> selectReviewsByCourseId(@Param("courseId") int courseId);

    // 내 활성 리뷰 존재 여부 확인 (delete_flg = 0)
    int countActiveReviewByCourseAndUser(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId
    );
    Long selectLastWatchedChapterId(@Param("userId") Long userId, @Param("courseId") int courseId);
}
