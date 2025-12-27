package com.learnit.learnit.courseDetail;

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

        setCommonModel(model, courseId, tab, false, false);
        return "courseDetail/courseDetail.html";
    }

    @GetMapping("/CourseDetailLogin")
    public String detailLogin(@RequestParam("courseId") int courseId,
                              @RequestParam(value = "tab", defaultValue = "intro") String tab,
                              Model model) {

        Long userId = 5L;

        if (courseDetailService.isEnrolled(userId, courseId)) {
            return "redirect:/CourseDetailStudy?courseId=" + courseId + "&tab=" + tab;
        }

        setCommonModel(model, courseId, tab, true, false);
        return "courseDetail/courseDetail.html";
    }

    @GetMapping("/CourseDetailStudy")
    public String detailStudy(@RequestParam("courseId") int courseId,
                              @RequestParam(value = "tab", defaultValue = "intro") String tab,
                              Model model) {

        Long userId = 5L;

        if (!courseDetailService.isEnrolled(userId, courseId)) {
            return "redirect:/CourseDetailLogin?courseId=" + courseId + "&tab=" + tab;
        }

        setCommonModel(model, courseId, tab, true, true);
        return "courseDetail/CourseDetailStudy.html"; // ✅ 파일명 그대로(대소문자 주의)
    }

    private void setCommonModel(Model model, int courseId, String tab,
                                boolean isLoggedIn, boolean isEnrolled) {

        CourseDTO course = courseDetailService.getCourse(courseId);

        model.addAttribute("course", course);
        model.addAttribute("activeTab", tab);

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isEnrolled", isEnrolled);

        model.addAttribute("userName", isLoggedIn ? "user" : null);

        List<ChapterDTO> chapters = courseDetailService.getChaptersOrDummy(courseId);
        model.addAttribute("chapters", chapters);

        // ✅ ✅ 3번 방식: 섹션별 map + 전체개수
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
