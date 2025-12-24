package com.learnit.learnit.courseDetail;

import com.learnit.learnit.course.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CourseDetailController {

    private final CourseDetailService courseDetailService;

    @GetMapping("/CourseDetail")
    public String detail(
            @RequestParam("courseId") int courseId,
            @RequestParam(value = "tab", defaultValue = "intro") String tab,
            Model model
    ) {
        CourseDTO course = courseDetailService.getCourse(courseId);

        model.addAttribute("course", course);
        model.addAttribute("activeTab", tab);

        // 탭별로 필요한 데이터만 세팅
        if ("reviews".equals(tab)) {
            model.addAttribute("reviews", courseDetailService.getDummyReviews());
        }

        // 커리큘럼도 DB 붙기 전이면 더미로라도 model에 담아줄 수 있음
        // if ("curriculum".equals(tab)) {
        //     model.addAttribute("curriculumList", courseDetailService.getDummyCurriculum());
        // }

        return "courseDetail/courseDetail.html";
    }
}
