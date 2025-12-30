package com.learnit.learnit.courseDetail;

import com.learnit.learnit.auth.AuthUtil;
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

        Long userId = AuthUtil.getLoginUserId();   // ✅ 로컬/소셜 통합
        boolean isLoggedIn = (userId != null);
        boolean isEnrolled = isLoggedIn && courseDetailService.isEnrolled(userId, courseId);

        setCommonModel(model, courseId, tab, isLoggedIn, isEnrolled);

        // (원하면 화면에서 쓰라고 userId도 내려줄 수 있음)
        model.addAttribute("loginUserId", userId);

        return "courseDetail/CourseDetail";
    }

    private void setCommonModel(Model model,
                                int courseId,
                                String tab,
                                boolean isLoggedIn,
                                boolean isEnrolled) {

        CourseDTO course = courseDetailService.getCourse(courseId);

        model.addAttribute("course", course);
        model.addAttribute("activeTab", tab);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isEnrolled", isEnrolled);

        List<ChapterDTO> chapters = courseDetailService.getChaptersOrDummy(courseId);
        model.addAttribute("chapters", chapters);

        Map<String, List<ChapterDTO>> sectionMap = courseDetailService.getCurriculumSectionMap(courseId);
        model.addAttribute("sectionMap", sectionMap);
        model.addAttribute("curriculumTotal", chapters.size());

        if ("reviews".equals(tab)) {
            model.addAttribute("reviews", courseDetailService.getDummyReviews());
        } else {
            model.addAttribute("reviews", Collections.emptyList());
        }
    }
}
