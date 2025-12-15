package com.learnit.learnit.course.controller;

import com.learnit.learnit.course.service.CategoryService;
import com.learnit.learnit.course.dto.CategoryDTO; // 네 DTO 이름에 맞게
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice          // ✅ 모든 @Controller 에 공통 적용
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CategoryService categoryService;

    // ✅ 모든 화면 렌더링 전에 항상 호출됨
    @ModelAttribute("categories")
    public List<CategoryDTO> categories() {
        return categoryService.getCategoryList();
    }
}
