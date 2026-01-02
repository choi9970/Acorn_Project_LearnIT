package com.learnit.learnit.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 리뷰 목록·검색·상태변경·삭제 처리 담당 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    /**
     * 리뷰 목록 페이지
     */
    @GetMapping
    public String reviewList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            Model model) {
        try {
            List<AdminReviewDto> reviews = adminReviewService.getReviews(page, size, status, search);
            int totalCount = adminReviewService.getReviewCount(status, search);
            int totalPages = (int) Math.ceil((double) totalCount / size);

            log.info("리뷰 목록 조회 완료: page={}, size={}, status={}, search={}, reviews.size()={}, totalCount={}", 
                page, size, status, search, reviews != null ? reviews.size() : 0, totalCount);

            model.addAttribute("reviews", reviews != null ? reviews : new java.util.ArrayList<>());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("currentStatus", status);
            model.addAttribute("searchKeyword", search);
            model.addAttribute("pageSize", size);
        } catch (Exception e) {
            log.error("리뷰 목록 조회 실패: page={}, size={}, status={}, search={}, error={}", 
                page, size, status, search, e.getMessage(), e);
            // 에러 발생 시 빈 리스트로 처리
            model.addAttribute("reviews", new java.util.ArrayList<>());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalCount", 0);
            model.addAttribute("currentStatus", status);
            model.addAttribute("searchKeyword", search);
            model.addAttribute("pageSize", size);
            model.addAttribute("errorMessage", "리뷰 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "admin/adminReviewList";
    }

    /**
     * 리뷰 상세 페이지
     */
    @GetMapping("/{reviewId}")
    public String reviewDetail(@PathVariable Long reviewId, Model model) {
        AdminReviewDto review = adminReviewService.getReview(reviewId);
        if (review == null) {
            return "redirect:/admin/review";
        }
        model.addAttribute("review", review);
        return "admin/adminReviewDetail";
    }

    /**
     * 리뷰 승인
     */
    @PostMapping("/{reviewId}/approve")
    public String approveReview(
            @PathVariable Long reviewId,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.approveReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 승인되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 승인 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/review";
    }

    /**
     * 리뷰 거부
     */
    @PostMapping("/{reviewId}/reject")
    public String rejectReview(
            @PathVariable Long reviewId,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.rejectReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 거부되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 거부 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/review";
    }

    /**
     * 리뷰 상태 변경
     */
    @PostMapping("/{reviewId}/update-status")
    public String updateStatus(
            @PathVariable Long reviewId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.updateStatus(reviewId, status);
            String statusMessage = "Active".equals(status) ? "대기" : 
                                 "Approved".equals(status) ? "승인" : "거부";
            redirectAttributes.addFlashAttribute("successMessage", "리뷰 상태가 '" + statusMessage + "'로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 상태 변경 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/review";
    }

    /**
     * 리뷰 삭제
     */
    @PostMapping("/{reviewId}/delete")
    public String deleteReview(
            @PathVariable Long reviewId,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.deleteReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/review";
    }
}

