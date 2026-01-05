package com.learnit.learnit.admin.notice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notice")
public class AdminNoticeController {

    private final AdminNoticeService service;

    @GetMapping
    public String list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            Model model
    ) {
        int totalCount = service.getTotalCount(category, search);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        if (totalPages <= 0) {
            totalPages = 1;
            page = 1;
        } else {
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
        }

        List<AdminNoticeDto> notices = service.getNotices(page, size, category, search);

        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentCategory", category);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("pageSize", size);

        return "admin/adminNoticeList";
    }

    // ✅ 글작성 폼: 목록 상태 유지용은 returnXXX로 받음(이름 충돌 방지)
    @GetMapping("/new")
    public String createForm(
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "10") int returnSize,
            @RequestParam(value = "returnCategory", required = false) String returnCategory,
            @RequestParam(value = "returnSearch", required = false) String returnSearch,
            Model model
    ) {
        model.addAttribute("mode", "create");
        model.addAttribute("notice", new AdminNoticeDto());

        model.addAttribute("returnPage", returnPage);
        model.addAttribute("returnSize", returnSize);
        model.addAttribute("returnCategory", returnCategory);
        model.addAttribute("returnSearch", returnSearch);

        return "admin/adminNoticeForm";
    }

    // ✅ 등록
    @PostMapping
    public String create(
            @ModelAttribute AdminNoticeDto notice,
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "10") int returnSize,
            @RequestParam(value = "returnCategory", required = false) String returnCategory,
            @RequestParam(value = "returnSearch", required = false) String returnSearch,
            RedirectAttributes ra
    ) {
        try {
            notice.setUserId(4); // ✅ 임시 고정
            service.create(notice);
            ra.addFlashAttribute("successMessage", "공지가 등록되었습니다.");

            addListParams(ra, returnPage, returnSize, returnCategory, returnSearch);
            return "redirect:/admin/notice";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());

            ra.addAttribute("returnPage", returnPage);
            ra.addAttribute("returnSize", returnSize);
            if (hasText(returnCategory)) ra.addAttribute("returnCategory", returnCategory);
            if (hasText(returnSearch)) ra.addAttribute("returnSearch", returnSearch);
            return "redirect:/admin/notice/new";
        }
    }

    // ✅ 수정 폼
    @GetMapping("/{noticeId}/edit")
    public String editForm(
            @PathVariable int noticeId,
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "10") int returnSize,
            @RequestParam(value = "returnCategory", required = false) String returnCategory,
            @RequestParam(value = "returnSearch", required = false) String returnSearch,
            Model model
    ) {
        AdminNoticeDto notice = service.getNotice(noticeId);
        if (notice == null) return "redirect:/admin/notice";

        model.addAttribute("mode", "edit");
        model.addAttribute("notice", notice);

        model.addAttribute("returnPage", returnPage);
        model.addAttribute("returnSize", returnSize);
        model.addAttribute("returnCategory", returnCategory);
        model.addAttribute("returnSearch", returnSearch);

        return "admin/adminNoticeForm";
    }

    // ✅ 수정 처리
    @PostMapping("/{noticeId}/update")
    public String update(
            @PathVariable int noticeId,
            @ModelAttribute AdminNoticeDto notice,
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "10") int returnSize,
            @RequestParam(value = "returnCategory", required = false) String returnCategory,
            @RequestParam(value = "returnSearch", required = false) String returnSearch,
            RedirectAttributes ra
    ) {
        try {
            notice.setNoticeId(noticeId);
            notice.setUserId(4); // ✅ 임시
            service.update(notice);
            ra.addFlashAttribute("successMessage", "공지가 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, returnPage, returnSize, returnCategory, returnSearch);
        return "redirect:/admin/notice";
    }

    // ✅ 개별 삭제
    @PostMapping("/{noticeId}/delete")
    public String delete(
            @PathVariable int noticeId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            RedirectAttributes ra
    ) {
        try {
            service.delete(noticeId);
            ra.addFlashAttribute("successMessage", "공지가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "삭제 실패: " + e.getMessage());
        }

        addListParams(ra, page, size, category, search);
        return "redirect:/admin/notice";
    }

    // ✅ 선택삭제 + 전체삭제(selectAll=true)
    @PostMapping("/delete-selected")
    public String deleteSelected(
            @RequestParam(value = "noticeIds", required = false) List<Integer> noticeIds,
            @RequestParam(value = "selectAll", defaultValue = "false") boolean selectAll,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            RedirectAttributes ra
    ) {
        try {
            if (selectAll) {
                service.deleteAllByFilter(category, search);
                ra.addFlashAttribute("successMessage", "조건에 해당하는 공지를 모두 삭제했습니다.");
                addListParams(ra, page, size, category, search);
                return "redirect:/admin/notice";
            }

            if (noticeIds == null || noticeIds.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "삭제할 공지를 선택해주세요.");
                addListParams(ra, page, size, category, search);
                return "redirect:/admin/notice";
            }

            service.deleteSelected(noticeIds);
            ra.addFlashAttribute("successMessage", "선택한 공지를 삭제했습니다.");

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "선택삭제 실패: " + e.getMessage());
        }

        addListParams(ra, page, size, category, search);
        return "redirect:/admin/notice";
    }

    private void addListParams(RedirectAttributes ra, int page, int size, String category, String search) {
        ra.addAttribute("page", page);
        ra.addAttribute("size", size);
        if (hasText(category)) ra.addAttribute("category", category);
        if (hasText(search)) ra.addAttribute("search", search);
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
