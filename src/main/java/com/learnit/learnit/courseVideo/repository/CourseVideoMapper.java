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
    
    /**
     * duration_sec > 0인 챕터 수 카운트 (수료증 발급용)
     */
    int countTotalChaptersWithDuration(@Param("courseId") Long courseId);

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
    
    /**
     * enrollment_id 조회
     */
    Long selectEnrollmentId(@Param("userId") Long userId, @Param("courseId") Long courseId);
    
    /**
     * enrollment 완강 처리 (completed_at 업데이트)
     */
    void updateEnrollmentCompleted(@Param("enrollmentId") Long enrollmentId);
    
    /**
     * 수료증 존재 여부 확인
     */
    boolean existsCertificate(@Param("enrollmentId") Long enrollmentId);
    
    /**
     * 사용자가 수강 중인 모든 강의 ID 조회
     */
    List<Long> selectEnrolledCourseIds(@Param("userId") Long userId);
    
    /**
     * 특정 챕터의 학습 시간 조회 (디버깅용)
     */
    Integer selectStudiedSec(@Param("userId") Long userId, @Param("chapterId") Long chapterId);
}
