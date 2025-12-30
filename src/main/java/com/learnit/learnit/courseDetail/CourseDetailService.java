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

    public List<ChapterDTO> getChaptersOrDummy(int courseId) {
        List<ChapterDTO> list = courseDetailMapper.selectChaptersByCourseId(courseId);
        if (list == null || list.isEmpty()) return getDummyChapters(courseId);
        return list;
    }

    // ✅ ✅ 3번 방식 핵심: 섹션별 그룹핑 Map
    public Map<String, List<ChapterDTO>> getCurriculumSectionMap(int courseId) {
        List<ChapterDTO> chapters = getChaptersOrDummy(courseId);

        Map<String, List<ChapterDTO>> sectionMap = new LinkedHashMap<>();
        for (ChapterDTO ch : chapters) {
            String key = ch.getSectionTitle();
            if (key == null || key.isBlank()) key = "섹션 1. 커리큘럼";

            sectionMap.computeIfAbsent(key, k -> new ArrayList<>()).add(ch);
        }
        return sectionMap;
    }

    // ✅ 전체 개수도 Controller에 주면 Thymeleaf가 더 안전함
    public int getCurriculumTotalCount(int courseId) {
        return getChaptersOrDummy(courseId).size();
    }

    public List<ChapterDTO> getDummyChapters(int courseId) {
        ChapterDTO c1 = new ChapterDTO();
        c1.setCourseId(courseId);
        c1.setOrderIndex(1);
        c1.setTitle("강의소개");
        c1.setSectionTitle("섹션 1. 커리큘럼");

        ChapterDTO c2 = new ChapterDTO();
        c2.setCourseId(courseId);
        c2.setOrderIndex(2);
        c2.setTitle("기본 개념");
        c2.setSectionTitle("섹션 1. 커리큘럼");

        ChapterDTO c3 = new ChapterDTO();
        c3.setCourseId(courseId);
        c3.setOrderIndex(3);
        c3.setTitle("실습");
        c3.setSectionTitle("섹션 2. 실습");

        return List.of(c1, c2, c3);
    }

    public List<Map<String, Object>> getDummyReviews() {
        return List.of(
                Map.of("name", "dmax", "rating", 5.0, "comment", "많은 도움이 되었습니다. 고맙습니다!"),
                Map.of("name", "Jang Jaehoon", "rating", 5.0, "comment", "좋은 강의 감사합니다!"),
                Map.of("name", "masiljangajji", "rating", 5.0, "comment", "Good")
        );
    }
}
