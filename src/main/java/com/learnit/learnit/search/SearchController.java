package com.learnit.learnit.search;

import com.learnit.learnit.course.CourseDTO;
import com.learnit.learnit.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final CourseMapper courseMapper;

    @GetMapping("/search")
    public String search(@RequestParam(value = "keyword", required = false) String keyword,
                         Model model) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return "redirect:/home";
        }

        // ✅ 매퍼 resultType이 CourseDTO니까 여기 타입도 CourseDTO로 맞춤
        List<CourseDTO> list = courseMapper.searchCourses(keyword);

        model.addAttribute("list", list);
        model.addAttribute("keyword", keyword);

        return "home/searchResult";
    }
}