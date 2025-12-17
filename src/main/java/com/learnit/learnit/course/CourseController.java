package com.learnit.learnit.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CourseController {

    @GetMapping("/CourseList")
    public String courseList(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            Model model
    ) {
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedTab", tab);
        model.addAttribute("selectedSort", sort);

        return "courseList/courseList";
    }
}
