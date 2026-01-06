package com.learnit.learnit.admin.qna;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private static final int PAGE_BLOCK_SIZE = 5;

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isSubAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUB_ADMIN"));
    }

    @GetMapping
    public String manage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "selectedId", required = false) Integer selectedId,
            Authentication auth,
            Model model
    ) {
        boolean admin = isAdmin(auth);
        boolean subAdmin = isSubAdmin(auth);

        // ✅ SUB_ADMIN이면 강의 Q&A만 강제
        if (subAdmin && !admin) {
            type = "LECTURE";
        }

        int totalCount = service.getTotalCount(type, status, search);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages <= 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        List<AdminQnaDto> list = service.getQnas(page, size, type, status, search);

        model.addAttribute("qnas", list);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);

        model.addAttribute("currentType", type);
        model.addAttribute("currentStatus", status);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("pageSize", size);

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        // ✅ 권한 플래그 (템플릿에서 버튼/폼 제어)
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isSubAdmin", subAdmin);

        if (selectedId != null) {
            AdminQnaDto detail = service.getDetail(selectedId);

            // ✅ 존재하지 않으면 선택 해제
            if (detail == null) {
                model.addAttribute("errorMessage", "존재하지 않는 Q&A 입니다.");
            } else {
                // ✅ SUB_ADMIN이 SITE(전체Q&A) 접근하면 차단
                if (subAdmin && !admin && detail.getCourseId() == null) {
                    model.addAttribute("errorMessage", "SUB_ADMIN은 강의 Q&A만 열람할 수 있습니다.");
                } else {
                    model.addAttribute("selectedId", selectedId);
                    model.addAttribute("selectedQna", detail);
                }
            }
        }

        return "admin/adminQnaManage";
    }

    @GetMapping("/{qnaId}")
    public String detailRedirect(
            @PathVariable int qnaId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            Authentication auth
    ) {
        // ✅ SUB_ADMIN이면 type 강제
        if (isSubAdmin(auth) && !isAdmin(auth)) {
            type = "LECTURE";
        }

        return "redirect:/admin/qna?selectedId=" + qnaId
                + (type != null ? "&type=" + type : "")
                + (status != null ? "&status=" + status : "")
                + (search != null ? "&search=" + search : "")
                + "&page=" + page
                + "&size=" + size;
    }

    // ✅ ADMIN만 가능
    @PostMapping("/{qnaId}/answer")
    public String saveAnswer(@PathVariable int qnaId,
                             @RequestParam("content") String content,
                             @RequestParam(value = "markResolved", defaultValue = "false") boolean markResolved,
                             @RequestParam(value = "type", required = false) String type,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "size", defaultValue = "7") int size,
                             Authentication auth,
                             RedirectAttributes ra) {

        if (!isAdmin(auth)) {
            ra.addFlashAttribute("errorMessage", "권한이 없습니다. (ADMIN 전용)");
            addListParams(ra, type, status, search, page, size, qnaId);
            return "redirect:/admin/qna";
        }

        try {
            int adminUserId = 4; // ✅ 임시 고정 (추후 로그인 사용자 id로 교체)
            service.saveAnswer(qnaId, adminUserId, content, markResolved);
            ra.addFlashAttribute("successMessage", "답변이 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, type, status, search, page, size, qnaId);
        return "redirect:/admin/qna";
    }

    // ✅ ADMIN만 가능
    @PostMapping("/{qnaId}/status")
    public String updateStatus(@PathVariable int qnaId,
                               @RequestParam("status") String uiStatus,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "statusFilter", required = false) String statusFilter,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "7") int size,
                               Authentication auth,
                               RedirectAttributes ra) {

        if (!isAdmin(auth)) {
            ra.addFlashAttribute("errorMessage", "권한이 없습니다. (ADMIN 전용)");
            addListParams(ra, type, statusFilter, search, page, size, qnaId);
            return "redirect:/admin/qna";
        }

        try {
            service.updateStatus(qnaId, uiStatus);
            ra.addFlashAttribute("successMessage", "상태가 변경되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, type, statusFilter, search, page, size, qnaId);
        return "redirect:/admin/qna";
    }

    // ✅ ADMIN만 가능
    @PostMapping("/{qnaId}/delete")
    public String delete(@PathVariable int qnaId,
                         @RequestParam(value = "type", required = false) String type,
                         @RequestParam(value = "status", required = false) String status,
                         @RequestParam(value = "search", required = false) String search,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         @RequestParam(value = "size", defaultValue = "7") int size,
                         Authentication auth,
                         RedirectAttributes ra) {

        if (!isAdmin(auth)) {
            ra.addFlashAttribute("errorMessage", "권한이 없습니다. (ADMIN 전용)");
            addListParams(ra, type, status, search, page, size, null);
            return "redirect:/admin/qna";
        }

        try {
            service.deleteQna(qnaId);
            ra.addFlashAttribute("successMessage", "Q&A가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "삭제 실패: " + e.getMessage());
        }

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
