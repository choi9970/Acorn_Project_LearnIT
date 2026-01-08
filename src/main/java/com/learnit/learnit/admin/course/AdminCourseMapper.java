package com.learnit.learnit.admin.course;

import com.learnit.learnit.admin.AdminChapterDTO;
import com.learnit.learnit.admin.AdminChapterInsertDTO;
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

    void insertChapter(AdminChapterInsertDTO chapter);

    void insertChapterResource(@Param("chapterId") Long chapterId,
                               @Param("title") String title,
                               @Param("fileUrl") String fileUrl,
                               @Param("fileType") String fileType);

    AdminCourseDetailDTO selectCourseById(Long courseId);
    
    List<AdminChapterDTO> selectChaptersWithResources(Long courseId);
    
    void updateCourse(AdminCourseCreateDTO course);
    
    void deleteChapterResourcesByCourseId(Long courseId);

    void deleteChaptersByCourseId(Long courseId);
    
    // Smart Update methods
    void updateChapter(AdminChapterInsertDTO chapter);
    void deleteChapter(Long chapterId);
    void updateChapterResource(@Param("resourceId") Long resourceId, 
                               @Param("title") String title,
                               @Param("fileUrl") String fileUrl,
                               @Param("fileType") String fileType);
    void deleteChapterResource(Long resourceId);
    void deleteChapterResourcesByChapterId(Long chapterId);
}
