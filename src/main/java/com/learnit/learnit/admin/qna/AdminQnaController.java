package com.learnit.learnit.admin.qna;

import jakarta.servlet.http.HttpSession;
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
    private final AdminQnaAuth auth;

    private static final int PAGE_BLOCK_SIZE = 5;

    private static final String FIELD_QNA_ID = "QNA_ID";
    private static final String FIELD_TITLE  = "TITLE";
    private static final String FIELD_WRITER = "WRITER";

    @GetMapping
    public String manage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "searchField", required = false) String searchField,
            @RequestParam(value = "search", required = false) String search,
            Model model,
            HttpSession session
    ) {
        boolean isAdmin = auth.isAdmin(session);
        boolean isSubAdmin = auth.isSubAdmin(session);

        type = normalize(type);
        status = normalize(status);
        searchField = normalize(searchField);
        search = normalize(search);

        if (searchField == null) searchField = FIELD_TITLE;
        if (!FIELD_QNA_ID.equals(searchField) && !FIELD_TITLE.equals(searchField) && !FIELD_WRITER.equals(searchField)) {
            searchField = FIELD_TITLE;
        }

        // ✅ SUB_ADMIN이면 강의Q&A만 + “본인 강의만”
        Long loginUserId = auth.loginUserId(session);
        Long instructorUserId = null;
        if (isSubAdmin && !isAdmin) {
            type = "LECTURE";
            instructorUserId = loginUserId; // ✅ 이게 핵심 (course.user_id = 본인)
        }

        if (size < 1) size = 7;
        if (page < 1) page = 1;

        Integer searchQnaId = null;
        if (FIELD_QNA_ID.equals(searchField) && search != null) {
            String s = search.trim();
            if (s.matches("^\\d+$")) {
                try { searchQnaId = Integer.parseInt(s); } catch (Exception ignored) {}
            }
        }

        int totalCount = service.getTotalCount(type, status, searchField, search, searchQnaId, instructorUserId);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages <= 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * size;
        List<AdminQnaDTO> qnas = service.getList(type, status, searchField, search, searchQnaId, offset, size, instructorUserId);

        int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        model.addAttribute("qnas", qnas);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isSubAdmin", isSubAdmin);

        model.addAttribute("currentType", type);
        model.addAttribute("currentStatus", status);

        model.addAttribute("searchField", searchField);
        model.addAttribute("searchKeyword", search);

        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "admin/qna/adminQnaManage";
    }
    // ✅✅✅ (신규) 강의 화면(/course/play)로 이동
    @GetMapping("/{qnaId}/go-course")
    public String goCourseQna(
            @PathVariable int qnaId,
            RedirectAttributes ra,
            HttpSession session
    ) {
        AdminQnaDTO detail = service.getDetail(qnaId);
        if (detail == null) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 Q&A 입니다.");
            return "redirect:/admin/qna";
        }

        if (!auth.canManageRow(session, detail.getCourseId())) {
            ra.addFlashAttribute("errorMessage", "권한이 없습니다.");
            return "redirect:/admin/qna";
        }

        if (detail.getCourseId() == null) {
            ra.addFlashAttribute("errorMessage", "전체 Q&A는 강의 화면으로 이동할 수 없습니다.");
            return "redirect:/admin/qna";
        }

        // SUB_ADMIN이면 내 강의만
        Long loginUserId = auth.loginUserId(session);
        if (auth.isSubAdmin(session) && !auth.isAdmin(session)) {
            Long ownerId = service.getInstructorUserIdByCourseId(detail.getCourseId());
            if (ownerId == null || loginUserId == null || !ownerId.equals(loginUserId)) {
                ra.addFlashAttribute("errorMessage", "권한이 없습니다. (내 강의 Q&A만 처리 가능)");
                return "redirect:/admin/qna";
            }
        }

        // ✅✅ chapterId가 필요하니까 "첫 챕터" 가져와서 붙임
        Integer chapterId = service.getFirstChapterIdByCourseId(detail.getCourseId());
        if (chapterId == null) {
            ra.addFlashAttribute("errorMessage", "강의 챕터 정보를 찾을 수 없습니다.");
            return "redirect:/admin/qna";
        }

        // ✅ 너가 보여준 URL 패턴 그대로
        String url = "/course/play?courseId=" + detail.getCourseId()
                + "&chapterId=" + chapterId
                + "&openQna=1&qnaId=" + qnaId;

        return "redirect:" + url;
    }

    // ✅ 답변+상태 저장
    @PostMapping("/{qnaId}/save")
    public String save(
            @PathVariable int qnaId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "newStatus", defaultValue = "ACTIVE") String newStatus,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "searchField", required = false) String searchField,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            RedirectAttributes ra,
            HttpSession session
    ) {
        type = normalize(type);
        status = normalize(status);
        searchField = normalize(searchField);
        search = normalize(search);

        Long loginUserId = auth.loginUserId(session);
        if (loginUserId == null) {
            ra.addFlashAttribute("errorMessage", "로그인 정보가 없습니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        AdminQnaDTO detail = service.getDetail(qnaId);
        if (detail == null) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 Q&A 입니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        // ✅ 기존 권한 체크
        if (!auth.canManageRow(session, detail.getCourseId())) {
            ra.addFlashAttribute("errorMessage", "권한이 없습니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        // ✅ 추가: SUB_ADMIN이면 “내 강의 Q&A만” 수정 가능
        if (auth.isSubAdmin(session) && !auth.isAdmin(session)) {
            Integer courseId = detail.getCourseId();
            if (courseId == null) {
                ra.addFlashAttribute("errorMessage", "권한이 없습니다.");
                return redirectList(ra, type, status, searchField, search, page, size);
            }
            Long ownerId = service.getInstructorUserIdByCourseId(courseId);
            if (ownerId == null || !ownerId.equals(loginUserId)) {
                ra.addFlashAttribute("errorMessage", "권한이 없습니다. (내 강의 Q&A만 처리 가능)");
                return redirectList(ra, type, status, searchField, search, page, size);
            }
        }

        service.saveAnswerAndStatus(qnaId, loginUserId, content, newStatus);
        ra.addFlashAttribute("successMessage", "저장되었습니다.");
        return redirectList(ra, type, status, searchField, search, page, size);
    }

    // ✅ 상태만 변경
    @PostMapping("/{qnaId}/status")
    public String changeStatus(
            @PathVariable int qnaId,
            @RequestParam(value = "newStatus", defaultValue = "ACTIVE") String newStatus,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "searchField", required = false) String searchField,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            RedirectAttributes ra,
            HttpSession session
    ) {
        type = normalize(type);
        status = normalize(status);
        searchField = normalize(searchField);
        search = normalize(search);

        Long loginUserId = auth.loginUserId(session);
        if (loginUserId == null) {
            ra.addFlashAttribute("errorMessage", "로그인 정보가 없습니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        AdminQnaDTO detail = service.getDetail(qnaId);
        if (detail == null) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 Q&A 입니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        if (!auth.canManageRow(session, detail.getCourseId())) {
            ra.addFlashAttribute("errorMessage", "권한이 없습니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        // ✅ SUB_ADMIN이면 “내 강의 Q&A만”
        if (auth.isSubAdmin(session) && !auth.isAdmin(session)) {
            Integer courseId = detail.getCourseId();
            Long ownerId = (courseId == null) ? null : service.getInstructorUserIdByCourseId(courseId);
            if (ownerId == null || !ownerId.equals(loginUserId)) {
                ra.addFlashAttribute("errorMessage", "권한이 없습니다. (내 강의 Q&A만 처리 가능)");
                return redirectList(ra, type, status, searchField, search, page, size);
            }
        }

        service.saveAnswerAndStatus(qnaId, loginUserId, null, newStatus);
        ra.addFlashAttribute("successMessage", "상태가 변경되었습니다.");
        return redirectList(ra, type, status, searchField, search, page, size);
    }

    // ✅ 삭제
    @PostMapping("/{qnaId}/delete")
    public String delete(
            @PathVariable int qnaId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "searchField", required = false) String searchField,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            RedirectAttributes ra,
            HttpSession session
    ) {
        type = normalize(type);
        status = normalize(status);
        searchField = normalize(searchField);
        search = normalize(search);

        Long loginUserId = auth.loginUserId(session);
        if (loginUserId == null) {
            ra.addFlashAttribute("errorMessage", "로그인 정보가 없습니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        AdminQnaDTO detail = service.getDetail(qnaId);
        if (detail == null) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 Q&A 입니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        if (!auth.canManageRow(session, detail.getCourseId())) {
            ra.addFlashAttribute("errorMessage", "권한이 없습니다.");
            return redirectList(ra, type, status, searchField, search, page, size);
        }

        // ✅ SUB_ADMIN이면 “내 강의 Q&A만”
        if (auth.isSubAdmin(session) && !auth.isAdmin(session)) {
            Integer courseId = detail.getCourseId();
            Long ownerId = (courseId == null) ? null : service.getInstructorUserIdByCourseId(courseId);
            if (ownerId == null || !ownerId.equals(loginUserId)) {
                ra.addFlashAttribute("errorMessage", "권한이 없습니다. (내 강의 Q&A만 처리 가능)");
                return redirectList(ra, type, status, searchField, search, page, size);
            }
        }

        service.delete(qnaId);
        ra.addFlashAttribute("successMessage", "삭제되었습니다.");
        return redirectList(ra, type, status, searchField, search, page, size);
    }

    private String redirectList(RedirectAttributes ra,
                                String type, String status, String searchField, String search,
                                int page, int size) {
        if (type != null && !type.isBlank()) ra.addAttribute("type", type);
        if (status != null && !status.isBlank()) ra.addAttribute("status", status);
        if (searchField != null && !searchField.isBlank()) ra.addAttribute("searchField", searchField);
        if (search != null && !search.isBlank()) ra.addAttribute("search", search);
        ra.addAttribute("page", page);
        ra.addAttribute("size", size);
        return "redirect:/admin/qna";
    }

    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (",".equals(t)) return null;
        return t;
    }
}
