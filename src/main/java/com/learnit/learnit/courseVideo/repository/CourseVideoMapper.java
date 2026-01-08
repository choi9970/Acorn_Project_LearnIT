package com.learnit.learnit.courseVideo.repository;

import com.learnit.learnit.courseVideo.dto.CourseFile;
import com.learnit.learnit.courseVideo.dto.CourseVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    List<CourseVideo> selectChapterList(@Param("courseId") Long courseId);

    List<CourseFile> selectCourseResources(@Param("courseId") Long courseId);

    String selectEnrollmentStatus(@Param("userId") Long userId, @Param("courseId") Long courseId);

    Long selectCourseInstructorId(@Param("courseId") Long courseId);

    void insertInterpreterLog(@Param("userId") Long userId,
                              @Param("courseId") Long courseId,
                              @Param("chapterId") Long chapterId,
                              @Param("languageId") Integer languageId);

    Long selectFirstChapterId(@Param("courseId") Long courseId);
}
