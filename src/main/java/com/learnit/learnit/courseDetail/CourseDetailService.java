package com.learnit.learnit.courseDetail;

import com.learnit.learnit.course.CourseDTO;
import com.learnit.learnit.enroll.EnrollmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseDetailService {

    private final CourseDetailMapper courseDetailMapper;
    private final EnrollmentMapper enrollmentMapper;

    public CourseDTO getCourse(int courseId) {
        return courseDetailMapper.selectCourseDetail(courseId);
    }

    public boolean isEnrolled(Long userId, int courseId) {
        return enrollmentMapper.countEnrollment(userId, courseId) > 0;
    }

    public List<ChapterDTO> getChapters(int courseId) {
        List<ChapterDTO> list = courseDetailMapper.selectChaptersByCourseId(courseId);
        return (list == null) ? Collections.emptyList() : list;
    }

    public Map<String, List<ChapterDTO>> getCurriculumSectionMap(int courseId) {
        List<ChapterDTO> chapters = getChapters(courseId);
        if (chapters.isEmpty()) return Collections.emptyMap();

        Map<String, List<ChapterDTO>> sectionMap = new LinkedHashMap<>();
        for (ChapterDTO ch : chapters) {
            String key = ch.getSectionTitle();
            if (key == null || key.isBlank()) key = "섹션 1. 커리큘럼";
            sectionMap.computeIfAbsent(key, k -> new ArrayList<>()).add(ch);
        }
        return sectionMap;
    }

    // ✅ 추가: 화면용 값들 (DTO에 안 넣고 model로 주입)
    public String getInstructorNameByUserId(Integer userId) {
        if (userId == null) return null;
        return courseDetailMapper.selectInstructorNameByUserId(userId);
    }

    public String getPeriodTextByCourseId(int courseId) {
        return courseDetailMapper.selectPeriodTextByCourseId(courseId);
    }

    public String getCategoryNameByCategoryId(Integer categoryId) {
        if (categoryId == null) return null;
        return courseDetailMapper.selectCategoryNameByCategoryId(categoryId);
    }

    /**
     * 강의별 리뷰 조회 (승인된 리뷰만)
     */
    public List<ReviewDTO> getReviews(int courseId) {
        List<ReviewDTO> reviews = courseDetailMapper.selectReviewsByCourseId(courseId);
        return (reviews == null) ? Collections.emptyList() : reviews;
    }
}
