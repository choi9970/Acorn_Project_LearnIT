package com.learnit.learnit.notice;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    // 목록 (예: /notice?page=1)
    @GetMapping("/notice")
    public String noticeList(@RequestParam(name = "page", defaultValue = "1") int page,
                             Model model) {

        int totalPages = noticeService.getTotalPages();
        int safePage = Math.max(1, Math.min(page, Math.max(totalPages, 1)));

        List<Notice> notices = noticeService.getNotices(safePage);

        // ✅ 페이지네이션: 1~5만 보여주기 (20개면 4페이지라 1~4)
        int startPage = 1;
        int endPage = Math.min(5, Math.max(totalPages, 1));

        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "notice/noticeList";
    }

    // 상세
    @GetMapping("/notice/{id}")
    public String noticeDetail(@PathVariable("id") Long id, Model model) {

        Notice notice = noticeService.getNotice(id);

        // (선택) 없는 글 처리 - 일단 간단히 목록으로
        if (notice == null) {
            return "redirect:/notice";
        }

        model.addAttribute("notice", notice);
        return "notice/noticeDetail";
    }
}
