package com.learnit.learnit.notice;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

@Controller
public class NoticeController {

    private final NoticeService noticeService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ✅ 페이지네이션 블록 크기 (1~5, 6~10 ...)
    private static final int PAGE_BLOCK_SIZE = 5;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notice")
    public String noticeList(@RequestParam(name = "page", defaultValue = "1") int page,
                             Model model) {

        int totalPages = noticeService.getTotalPages();
        if (totalPages <= 0) totalPages = 1;

        int safePage = Math.max(1, Math.min(page, totalPages));
        List<Notice> notices = noticeService.getNotices(safePage);

        // ✅ 블록 계산 (현재 페이지가 속한 5개 묶음)
        int startPage = ((safePage - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        // ✅ 이전/다음 블록 이동용 페이지
        Integer prevBlockPage = (startPage > 1) ? (startPage - 1) : null;
        Integer nextBlockPage = (endPage < totalPages) ? (endPage + 1) : null;

        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        model.addAttribute("prevBlockPage", prevBlockPage);
        model.addAttribute("nextBlockPage", nextBlockPage);

        return "notice/noticeList";
    }

    @GetMapping("/notice/{id}")
    public String noticeDetail(
            @PathVariable("id") Long id,
            // ✅ 관리자 목록에서 들어왔을 때만 넘어오는 값들
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "7") int returnSize,
            @RequestParam(value = "returnCategory", required = false) String returnCategory,
            @RequestParam(value = "returnSearch", required = false) String returnSearch,
            Model model
    ) {
        Notice notice = noticeService.getNotice(id);
        if (notice == null) return "redirect:/notice";

        model.addAttribute("notice", notice);

        // ✅ 관리자에서 왔는지 여부 + 돌아갈 목록 상태
        model.addAttribute("from", from);
        model.addAttribute("returnPage", returnPage);
        model.addAttribute("returnSize", returnSize);
        model.addAttribute("returnCategory", returnCategory);
        model.addAttribute("returnSearch", returnSearch);

        return "notice/noticeDetail";
    }

    // ✅ 첨부파일 다운로드
    @GetMapping("/notice/{id}/download")
    public void downloadNotice(@PathVariable Long id, HttpServletResponse response) {
        Notice notice = noticeService.getNotice(id);

        try {
            if (notice == null || notice.getFileUrl() == null || notice.getFileUrl().isBlank()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // fileUrl: /uploads/notice/uuid__원본.txt
            String storedName = Paths.get(notice.getFileUrl()).getFileName().toString();
            Path filePath = Paths.get(uploadDir, "notice", storedName).toAbsolutePath().normalize();

            if (!Files.exists(filePath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String originalName = notice.getOriginalFileName() != null ? notice.getOriginalFileName() : storedName;
            String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);

            try (InputStream is = Files.newInputStream(filePath);
                 OutputStream os = response.getOutputStream()) {
                is.transferTo(os);
                os.flush();
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
