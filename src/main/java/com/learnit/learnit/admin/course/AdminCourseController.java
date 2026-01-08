package com.learnit.learnit.admin.course;

import com.learnit.learnit.category.CategoryService;
import com.learnit.learnit.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/course")
@RequiredArgsConstructor
public class AdminCourseController {

    private final AdminCourseService adminCourseService;
    private final CategoryService categoryService;

    @org.springframework.web.bind.annotation.InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new org.springframework.beans.propertyeditors.StringTrimmerEditor(true));
    }

    @GetMapping("/api/instructors/search")
    @ResponseBody
    public List<UserDTO> searchInstructors(@RequestParam("keyword") String keyword) {
        return adminCourseService.searchInstructors(keyword);
    }

    @GetMapping
    public String courseList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            Model model) {
        
        // 빈 문자열인 경우 null로 처리하여 필터가 적용되지 않도록 함
        if ("".equals(status)) {
            status = null;
        }

        try {
            List<AdminCourse> courses = adminCourseService.getCourses(page, size, status, search);
            int totalCount = adminCourseService.getCourseCount(status, search);
            int totalPages = (int) Math.ceil((double) totalCount / size);

            // 페이징 그룹 로직 (5페이지씩 표시)
            int pageGroupSize = 5;
            int startPage = ((page - 1) / pageGroupSize) * pageGroupSize + 1;
            int endPage = Math.min(startPage + pageGroupSize - 1, totalPages);
            if (endPage == 0) endPage = 1; // 페이지가 0개일 경우 처리

            model.addAttribute("courses", courses != null ? courses : new java.util.ArrayList<>());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("currentStatus", status);
            model.addAttribute("searchKeyword", search);
            model.addAttribute("pageSize", size);
        } catch (Exception e) {
            log.error("강의 목록 조회 실패: page={}, size={}, status={}, search={}, error={}", 
                page, size, status, search, e.getMessage(), e);
            model.addAttribute("courses", new java.util.ArrayList<>());
            model.addAttribute("errorMessage", "강의 목록을 불러오는 중 오류가 발생했습니다.");
        }

        return "admin/adminCourseList";
    }

    @PostMapping("/{courseId}/delete")
    public String deleteCourse(
            @PathVariable Long courseId,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("강의 삭제 요청: courseId={}", courseId);
            adminCourseService.deleteCourse(courseId);
            redirectAttributes.addFlashAttribute("successMessage", "강의가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("강의 삭제 실패: courseId={}, error={}", courseId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "강의 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/course";
    }

    @GetMapping("/create")
    public String createCourseForm(Model model) {
        model.addAttribute("categories", categoryService.getCategoryList());
        return "admin/adminCourseForm";
    }

    @PostMapping("/create")
    public String createCourse(AdminCourseCreateDTO dto, RedirectAttributes redirectAttributes) {
        try {
            log.info("강의 생성 요청: {}", dto);
            adminCourseService.createCourse(dto);
            redirectAttributes.addFlashAttribute("successMessage", "강의가 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            log.error("강의 생성 실패: error={}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "강의 등록 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/course";
    }

    @GetMapping("/{courseId}/edit")
    public String editCourseForm(@PathVariable Long courseId, Model model) {
        try {
            AdminCourseCreateDTO course = adminCourseService.getCourseDetail(courseId);
            model.addAttribute("course", course);
            model.addAttribute("categories", categoryService.getCategoryList());
            // 수정 모드임을 뷰에 알림
            model.addAttribute("isEdit", true);
            
            return "admin/adminCourseForm";
        } catch (Exception e) {
            log.error("강의 수정 폼 로딩 실패", e);
            return "redirect:/admin/course";
        }
    }

    @PostMapping("/{courseId}/edit")
    public String updateCourse(@PathVariable Long courseId, AdminCourseCreateDTO dto, RedirectAttributes redirectAttributes) {
        try {
            log.info("강의 수정 요청: courseId={}, dto={}", courseId, dto);
            adminCourseService.updateCourse(courseId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "강의가 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            log.error("강의 수정 실패: error={}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "강의 수정 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/course";
    }
}
