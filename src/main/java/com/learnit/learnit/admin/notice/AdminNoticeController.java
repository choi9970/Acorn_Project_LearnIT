package com.learnit.learnit.admin.notice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notice")
public class AdminNoticeController {

    private final AdminNoticeService service;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final int PAGE_BLOCK_SIZE = 5;

    @GetMapping
    public String list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            Model model
    ) {
        int totalCount = service.getTotalCount(category, search);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages <= 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        List<AdminNoticeDto> notices = service.getNotices(page, size, category, search);

        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentCategory", category);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("pageSize", size);

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "admin/adminNoticeList";
    }

    @GetMapping("/new")
    public String createForm(
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "7") int returnSize,
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

    @PostMapping
    public String create(
            @ModelAttribute AdminNoticeDto notice,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "7") int returnSize,
            @RequestParam(value = "returnCategory", required = false) String returnCategory,
            @RequestParam(value = "returnSearch", required = false) String returnSearch,
            RedirectAttributes ra
    ) {
        try {
            notice.setUserId(4); // 임시

            if (file != null && !file.isEmpty()) {
                String savedUrl = saveNoticeFile(file);
                notice.setFileUrl(savedUrl);
            }

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

    @GetMapping("/{noticeId}/edit")
    public String editForm(
            @PathVariable int noticeId,
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "7") int returnSize,
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

    @PostMapping("/{noticeId}/update")
    public String update(
            @PathVariable int noticeId,
            @ModelAttribute AdminNoticeDto notice,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "deleteFile", defaultValue = "false") boolean deleteFile,
            @RequestParam(value = "returnPage", defaultValue = "1") int returnPage,
            @RequestParam(value = "returnSize", defaultValue = "7") int returnSize,
            @RequestParam(value = "returnCategory", required = false) String returnCategory,
            @RequestParam(value = "returnSearch", required = false) String returnSearch,
            RedirectAttributes ra
    ) {
        try {
            notice.setNoticeId(noticeId);
            notice.setUserId(4); // 임시

            AdminNoticeDto origin = service.getNotice(noticeId);
            String originUrl = (origin != null) ? origin.getFileUrl() : null;

            notice.setFileUrl(originUrl);

            if (deleteFile && hasText(originUrl)) {
                deletePhysicalFile(originUrl);
                originUrl = null;
                notice.setFileUrl(null);
            }

            if (file != null && !file.isEmpty()) {
                if (hasText(originUrl)) {
                    deletePhysicalFile(originUrl);
                }
                String savedUrl = saveNoticeFile(file);
                notice.setFileUrl(savedUrl);
            }

            service.update(notice);
            ra.addFlashAttribute("successMessage", "공지가 수정되었습니다.");

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, returnPage, returnSize, returnCategory, returnSearch);
        return "redirect:/admin/notice";
    }

    /**
     * ✅ (추가) 단건 삭제 매핑
     * 화면에서 POST /admin/notice/{id}/delete 로 보내는걸 받아줌
     */
    @PostMapping("/{noticeId}/delete")
    public String delete(
            @PathVariable int noticeId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "7") int size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            RedirectAttributes ra
    ) {
        try {
            // ✅ 첨부파일도 같이 지우고 싶으면(권장)
            AdminNoticeDto origin = service.getNotice(noticeId);
            if (origin != null && hasText(origin.getFileUrl())) {
                deletePhysicalFile(origin.getFileUrl());
            }

            service.delete(noticeId);
            ra.addFlashAttribute("successMessage", "공지가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        addListParams(ra, page, size, category, search);
        return "redirect:/admin/notice";
    }

    private String saveNoticeFile(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        if (original == null) original = "file";
        original = sanitizeFilename(original);

        String savedName = UUID.randomUUID().toString().replace("-", "") + "__" + original;

        Path dir = Paths.get(uploadDir, "notice").toAbsolutePath().normalize();
        Files.createDirectories(dir);

        Path target = dir.resolve(savedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/notice/" + savedName;
    }

    private void deletePhysicalFile(String fileUrl) {
        try {
            String storedName = Paths.get(fileUrl).getFileName().toString(); // uuid__원본
            Path path = Paths.get(uploadDir, "notice", storedName).toAbsolutePath().normalize();
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("첨부파일 삭제 실패: {}", e.getMessage());
        }
    }

    private String sanitizeFilename(String name) {
        name = name.replace("\\", "_").replace("/", "_");
        name = name.replaceAll("[\\r\\n\\t]", "_");
        return name;
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
