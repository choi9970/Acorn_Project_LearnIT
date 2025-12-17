package com.learnit.learnit.course;

import com.learnit.learnit.common.PageResponse;
import com.learnit.learnit.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;

    public PageResponse<CourseDTO> getCoursesPage(Integer categoryId, String sort, String tab, int page, int size) {

        String safeSort = (sort == null) ? "latest" : sort;
        String safeTab  = (tab == null)  ? "all"    : tab;

        if (!safeSort.matches("latest|popular|priceAsc|priceDesc")) safeSort = "latest";
        if (!safeTab.matches("all|free")) safeTab = "all";

        if (page < 0) page = 0;
        if (size <= 0) size = 12;

        int offset = page * size;

        List<CourseDTO> content =
                courseMapper.selectCoursesPage(categoryId, safeSort, safeTab, size, offset);

        long total =
                courseMapper.countCourses(categoryId, safeTab);

        int totalPages = (int) Math.ceil((double) total / size);
        boolean last = (totalPages == 0) || (page + 1 >= totalPages);

        return new PageResponse<>(
                content,
                page,
                size,
                total,
                totalPages,
                last
        );
    }
}
