package com.learnit.learnit.course.repository;

import com.learnit.learnit.course.dto.CourseVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseVideoMapper {
    CourseVideo findById(@Param("chapterId") Long chapterId);

    Long selectPrevChapterId(@Param("courseId") Long courseId, @Param("orderIndex") Long orderIndex);

    Long selectNextChapterId(@Param("courseId") Long courseId, @Param("orderIndex") Long orderIndex);

    void insertOrUpdateStudyLog(@Param("userId") Long userId,
                                @Param("courseId") Long courseId,
                                @Param("chapterId") Long chapterId,
                                @Param("playTime") Integer playTime);

    int countTotalChapters(@Param("courseId") Long courseId);

    int countCompletedChapters(@Param("userId") Long userId, @Param("courseId") Long courseId);

    void updateChapterDuration(@Param("chapterId") Long chapterId, @Param("duration") int duration);
}
