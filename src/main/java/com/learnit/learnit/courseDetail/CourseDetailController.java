package com.learnit.learnit.courseDetail;

import com.learnit.learnit.course.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

@Controller
@RequiredArgsConstructor
public class CourseDetailController {

    private final CourseDetailService courseDetailService;

    // ✅ 비로그인 상세
    @GetMapping("/CourseDetail")
    public String detail(
            @RequestParam("courseId") int courseId,
            @RequestParam(value = "tab", defaultValue = "intro") String tab,
            Model model
    ) {
        setCommonModel(model, courseId, tab, false);
        return "courseDetail/courseDetail.html";
    }

    // ✅ 로그인 가정 상세
    @GetMapping("/CourseDetailLogin")
    public String detailLogin(
            @RequestParam("courseId") int courseId,
            @RequestParam(value = "tab", defaultValue = "intro") String tab,
            Model model
    ) {
        setCommonModel(model, courseId, tab, true);
        return "courseDetail/courseDetail.html";
    }

    private void setCommonModel(Model model, int courseId, String tab, boolean isLoggedIn) {
        CourseDTO course = courseDetailService.getCourse(courseId);

        model.addAttribute("course", course);
        model.addAttribute("activeTab", tab);
        model.addAttribute("isLoggedIn", isLoggedIn);

        // ✅ 로그인 가정 유저명(나중에 세션/시큐리티로 바꾸면 됨)
        model.addAttribute("userName", isLoggedIn ? "user" : null);

        // ✅ chapters는 항상 담기
        model.addAttribute("chapters", courseDetailService.getChaptersOrDummy(courseId));

        // ✅ reviews는 reviews 탭일 때만
        if ("reviews".equals(tab)) {
            model.addAttribute("reviews", courseDetailService.getDummyReviews());
        } else {
            model.addAttribute("reviews", Collections.emptyList());
        }
    }
}
