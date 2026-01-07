package com.learnit.learnit.notice;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

@Controller
public class NoticeController {

    private final NoticeService noticeService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notice")
    public String noticeList(@RequestParam(name = "page", defaultValue = "1") int page,
                             Model model) {

        int totalPages = noticeService.getTotalPages();
        int safePage = Math.max(1, Math.min(page, Math.max(totalPages, 1)));

        List<Notice> notices = noticeService.getNotices(safePage);

        int startPage = 1;
        int endPage = Math.min(5, Math.max(totalPages, 1));

        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "notice/noticeList";
    }

    @GetMapping("/notice/{id}")
    public String noticeDetail(@PathVariable("id") Long id, Model model) {

        Notice notice = noticeService.getNotice(id);
        if (notice == null) return "redirect:/notice";

        model.addAttribute("notice", notice);
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

            // ✅ 원본 파일명으로 다운로드되게 헤더 세팅
            String originalName = notice.getOriginalFileName() != null ? notice.getOriginalFileName() : storedName;

            String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);

            // 파일 스트림 복사
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
