package com.learnit.learnit.admin.qna;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/qna")
public class AdminQnaController {

    private final AdminQnaService service;

    @GetMapping
    public String manage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            Model model
    ) {
        List<AdminQnaDto> list = service.getQnas(page, size, type, status, search);
        int totalCount = service.getTotalCount(type, status, search);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        model.addAttribute("qnas", list);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);

        model.addAttribute("currentType", type);
        model.addAttribute("currentStatus", status);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("pageSize", size);

        return "admin/adminQnaManage";
    }

    @GetMapping("/{qnaId}")
    public String detail(@PathVariable int qnaId,
                         @RequestParam(value = "type", required = false) String type,
                         @RequestParam(value = "status", required = false) String status,
                         @RequestParam(value = "search", required = false) String search,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         @RequestParam(value = "size", defaultValue = "10") int size,
                         Model model) {

        model.addAttribute("qna", service.getDetail(qnaId));

        model.addAttribute("currentType", type);
        model.addAttribute("currentStatus", status);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "admin/adminQnaDetail";
    }

    @PostMapping("/{qnaId}/answer")
    public String saveAnswer(@PathVariable int qnaId,
                             @RequestParam("content") String content,
                             @RequestParam(value = "markResolved", defaultValue = "true") boolean markResolved,
                             @RequestParam(value = "type", required = false) String type,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size,
                             RedirectAttributes ra) {
        try {
            int adminUserId = 4; // ✅ 임시 고정
            service.saveAnswer(qnaId, adminUserId, content, markResolved);
            ra.addFlashAttribute("successMessage", "답변이 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, type, status, search, page, size);
        return "redirect:/admin/qna/" + qnaId;
    }

    @PostMapping("/{qnaId}/status")
    public String updateStatus(@PathVariable int qnaId,
                               @RequestParam("status") String uiStatus,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "statusFilter", required = false) String statusFilter,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               RedirectAttributes ra) {
        try {
            service.updateStatus(qnaId, uiStatus);
            ra.addFlashAttribute("successMessage", "상태가 변경되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        // statusFilter는 목록 필터용(이름 충돌 방지)
        addListParams(ra, type, statusFilter, search, page, size);
        return "redirect:/admin/qna/" + qnaId;
    }

    // ✅✅ 추가: 삭제
    @PostMapping("/{qnaId}/delete")
    public String delete(@PathVariable int qnaId,
                         @RequestParam(value = "type", required = false) String type,
                         @RequestParam(value = "status", required = false) String status,
                         @RequestParam(value = "search", required = false) String search,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         @RequestParam(value = "size", defaultValue = "10") int size,
                         RedirectAttributes ra) {
        try {
            service.deleteQna(qnaId);
            ra.addFlashAttribute("successMessage", "Q&A가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "삭제 실패: " + e.getMessage());
        }

        addListParams(ra, type, status, search, page, size);
        return "redirect:/admin/qna";
    }

    private void addListParams(RedirectAttributes ra, String type, String status, String search, int page, int size) {
        ra.addAttribute("page", page);
        ra.addAttribute("size", size);
        if (type != null && !type.isBlank()) ra.addAttribute("type", type);
        if (status != null && !status.isBlank()) ra.addAttribute("status", status);
        if (search != null && !search.isBlank()) ra.addAttribute("search", search);
    }
}
