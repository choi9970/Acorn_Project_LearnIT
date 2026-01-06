package com.learnit.learnit.admin.qna;

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
@RequestMapping("/admin/qna")
public class AdminQnaController {

    private final AdminQnaService service;

    private static final int PAGE_BLOCK_SIZE = 5;

    @GetMapping
    public String manage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "selectedId", required = false) Integer selectedId,
            Model model
    ) {
        try {
            int totalCount = service.getTotalCount(type, status, search);
            int totalPages = (int) Math.ceil((double) totalCount / size);
            if (totalPages <= 0) totalPages = 1;

            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
            int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

            List<AdminQnaDto> list = service.getQnas(page, size, type, status, search);

            model.addAttribute("qnas", list != null ? list : new java.util.ArrayList<>());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalCount", totalCount);

            model.addAttribute("currentType", type);
            model.addAttribute("currentStatus", status);
            model.addAttribute("searchKeyword", search);
            model.addAttribute("pageSize", size);

            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);

            if (selectedId != null) {
                try {
                    model.addAttribute("selectedId", selectedId);
                    AdminQnaDto selectedQna = service.getDetail(selectedId);
                    if (selectedQna != null) {
                        model.addAttribute("selectedQna", selectedQna);
                    }
                } catch (Exception e) {
                    log.error("QnA 상세 조회 실패: selectedId={}, error={}", selectedId, e.getMessage());
                    // selectedQna는 추가하지 않음
                }
            }
        } catch (Exception e) {
            model.addAttribute("qnas", new java.util.ArrayList<>());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalCount", 0);
            model.addAttribute("currentType", type);
            model.addAttribute("currentStatus", status);
            model.addAttribute("searchKeyword", search);
            model.addAttribute("pageSize", size);
            model.addAttribute("startPage", 1);
            model.addAttribute("endPage", 0);
            model.addAttribute("errorMessage", "Q&A 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "admin/adminQnaManage";
    }

    // ✅ 기존 /{qnaId} 접근 들어와도 "같은 페이지(selectedId)"로 보내기
    @GetMapping("/{qnaId}")
    public String detailRedirect(
            @PathVariable int qnaId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size
    ) {
        return "redirect:/admin/qna?selectedId=" + qnaId
                + (type != null ? "&type=" + type : "")
                + (status != null ? "&status=" + status : "")
                + (search != null ? "&search=" + search : "")
                + "&page=" + page
                + "&size=" + size;
    }

    @PostMapping("/{qnaId}/answer")
    public String saveAnswer(@PathVariable int qnaId,
                             @RequestParam("content") String content,
                             @RequestParam(value = "markResolved", defaultValue = "true") boolean markResolved,
                             @RequestParam(value = "type", required = false) String type,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "size", defaultValue = "7") int size,
                             RedirectAttributes ra) {
        try {
            int adminUserId = 4; // ✅ 임시 고정
            service.saveAnswer(qnaId, adminUserId, content, markResolved);
            ra.addFlashAttribute("successMessage", "답변이 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, type, status, search, page, size, qnaId);
        return "redirect:/admin/qna";
    }

    @PostMapping("/{qnaId}/status")
    public String updateStatus(@PathVariable int qnaId,
                               @RequestParam("status") String uiStatus,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "statusFilter", required = false) String statusFilter,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "7") int size,
                               RedirectAttributes ra) {
        try {
            service.updateStatus(qnaId, uiStatus);
            ra.addFlashAttribute("successMessage", "상태가 변경되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, type, statusFilter, search, page, size, qnaId);
        return "redirect:/admin/qna";
    }

    @PostMapping("/{qnaId}/delete")
    public String delete(@PathVariable int qnaId,
                         @RequestParam(value = "type", required = false) String type,
                         @RequestParam(value = "status", required = false) String status,
                         @RequestParam(value = "search", required = false) String search,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         @RequestParam(value = "size", defaultValue = "7") int size,
                         RedirectAttributes ra) {
        try {
            service.deleteQna(qnaId);
            ra.addFlashAttribute("successMessage", "Q&A가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "삭제 실패: " + e.getMessage());
        }

        // 삭제 후 선택 해제
        addListParams(ra, type, status, search, page, size, null);
        return "redirect:/admin/qna";
    }

    private void addListParams(RedirectAttributes ra, String type, String status, String search,
                               int page, int size, Integer selectedId) {
        ra.addAttribute("page", page);
        ra.addAttribute("size", size);
        if (type != null && !type.isBlank()) ra.addAttribute("type", type);
        if (status != null && !status.isBlank()) ra.addAttribute("status", status);
        if (search != null && !search.isBlank()) ra.addAttribute("search", search);
        if (selectedId != null) ra.addAttribute("selectedId", selectedId);
    }
}
