package com.learnit.learnit.courseDetail;

import com.learnit.learnit.course.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseDetailService {

    private final CourseDetailMapper courseDetailMapper;

    public CourseDTO getCourse(int courseId) {
        return courseDetailMapper.selectCourseDetail(courseId);
    }

    // ✅ 커리큘럼 더미
    public List<Map<String, Object>> getDummyCurriculum() {
        return List.of(
                Map.of("title", "1강. OT"),
                Map.of("title", "2강. 기본 개념"),
                Map.of("title", "3강. 실습")
        );
    }

    // ✅ 수강평 더미
    public List<Map<String, Object>> getDummyReviews() {
        return List.of(
                Map.of("name", "dmax", "rating", 5.0, "comment", "많은 도움이 되었습니다. 고맙습니다!"),
                Map.of("name", "Jang Jaehoon", "rating", 5.0, "comment", "좋은 강의 감사합니다!"),
                Map.of("name", "masiljangajji", "rating", 5.0, "comment", "Good")
        );
    }
}
