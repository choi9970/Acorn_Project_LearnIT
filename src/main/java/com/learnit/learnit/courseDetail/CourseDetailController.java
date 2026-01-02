package com.learnit.learnit.courseDetail;

import com.learnit.learnit.user.util.SessionUtils;
import com.learnit.learnit.course.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CourseDetailController {

    private final CourseDetailService courseDetailService;

    @GetMapping("/CourseDetail")
    public String detail(@RequestParam("courseId") int courseId,
                         @RequestParam(value = "tab", defaultValue = "intro") String tab,
                         Model model) {

        try {
            Long loginUserId = SessionUtils.getLoginUserId();
            boolean isLoggedIn = (loginUserId != null);
            boolean isEnrolled = isLoggedIn && courseDetailService.isEnrolled(loginUserId, courseId);

            setCommonModel(model, courseId, tab, isLoggedIn, isEnrolled, loginUserId);
            return "courseDetail/CourseDetail";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "강의 상세 정보를 불러오는 중 오류가 발생했습니다.");
            return "error/500";
        }
    }

    private void setCommonModel(Model model,
                                int courseId,
                                String tab,
                                boolean isLoggedIn,
                                boolean isEnrolled,
                                Long loginUserId) {

        CourseDTO course = courseDetailService.getCourse(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course not found. courseId=" + courseId);
        }

        // ✅ DTO에 없는 화면용 값들
        String instructorName = courseDetailService.getInstructorNameByUserId(course.getUserId());
        String periodText = courseDetailService.getPeriodTextByCourseId(courseId);
        String categoryName = courseDetailService.getCategoryNameByCategoryId(course.getCategoryId());

        // ✅ null 방어 (화면 깨짐 방지)
        if (instructorName == null) instructorName = "지식공유자";
        if (periodText == null) periodText = "무제한";
        if (categoryName == null) categoryName = "카테고리";

        model.addAttribute("course", course);
        model.addAttribute("activeTab", tab);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("loginUserId", loginUserId);

        model.addAttribute("instructorName", instructorName);
        model.addAttribute("periodText", periodText);
        model.addAttribute("categoryName", categoryName);

        // 커리큘럼
        List<ChapterDTO> chapters = courseDetailService.getChapters(courseId);
        model.addAttribute("chapters", chapters);

        Map<String, List<ChapterDTO>> sectionMap = courseDetailService.getCurriculumSectionMap(courseId);
        model.addAttribute("sectionMap", sectionMap);

        model.addAttribute("curriculumTotal", chapters.size());

        // 리뷰 조회 (승인된 리뷰만 표시)
        List<ReviewDTO> reviews = courseDetailService.getReviews(courseId);
        model.addAttribute("reviews", reviews != null ? reviews : Collections.emptyList());
    }
}
