package com.learnit.learnit.search;

import com.learnit.learnit.course.CourseDTO;
import com.learnit.learnit.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchApiController {

    private final CourseMapper courseMapper;

    @GetMapping("/courses")
    public List<CourseDTO> searchCourses(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size
    ) {
        // ✅ 1차 버전: 기존 searchCourses 재활용 (페이징은 프론트에서만)
        return courseMapper.searchCourses(keyword);
    }
}