package com.learnit.learnit.admin;

import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
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
            @RequestParam(value = "size", defaultValue = "7") int size,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "search", required = false) String search,
            HttpSession session,
            Model model) {
        try {
            Long userId = SessionUtils.getUserId(session);
            List<AdminReviewDto> reviews = adminReviewService.getReviews(page, size, searchType, search, userId);
            int totalCount = adminReviewService.getReviewCount(searchType, search, userId);
            int totalPages = (int) Math.ceil((double) totalCount / size);

            log.info("리뷰 목록 조회 완료: page={}, size={}, searchType={}, search={}, userId={}, reviews.size()={}, totalCount={}", 
                page, size, searchType, search, userId, reviews != null ? reviews.size() : 0, totalCount);

            model.addAttribute("reviews", reviews != null ? reviews : new java.util.ArrayList<>());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("currentSearchType", searchType);
            model.addAttribute("searchKeyword", search);
            model.addAttribute("pageSize", size);
        } catch (Exception e) {
            log.error("리뷰 목록 조회 실패: page={}, size={}, searchType={}, search={}, error={}", 
                page, size, searchType, search, e.getMessage(), e);
            // 에러 발생 시 빈 리스트로 처리
            model.addAttribute("reviews", new java.util.ArrayList<>());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalCount", 0);
            model.addAttribute("currentSearchType", searchType);
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
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.approveReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 승인되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 승인 중 오류가 발생했습니다.");
        }
        
        // 페이지 파라미터 유지
        if (page != null && page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
        if (searchType != null && !searchType.isEmpty()) {
            redirectAttributes.addAttribute("searchType", searchType);
        }
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
        }
        
        return "redirect:/admin/review";
    }

    /**
     * 리뷰 거부
     */
    @PostMapping("/{reviewId}/reject")
    public String rejectReview(
            @PathVariable Long reviewId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.rejectReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 거부되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 거부 중 오류가 발생했습니다.");
        }
        
        // 페이지 파라미터 유지
        if (page != null && page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
        if (searchType != null && !searchType.isEmpty()) {
            redirectAttributes.addAttribute("searchType", searchType);
        }
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
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
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "search", required = false) String search,
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
        
        // 페이지 파라미터 유지
        if (page != null && page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
        if (searchType != null && !searchType.isEmpty()) {
            redirectAttributes.addAttribute("searchType", searchType);
        }
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
        }
        
        return "redirect:/admin/review";
    }

    /**
     * 리뷰 삭제
     */
    @PostMapping("/{reviewId}/delete")
    public String deleteReview(
            @PathVariable Long reviewId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.deleteReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 삭제 중 오류가 발생했습니다.");
        }
        
        // 페이지 파라미터 유지
        if (page != null && page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
        if (searchType != null && !searchType.isEmpty()) {
            redirectAttributes.addAttribute("searchType", searchType);
        }
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
        }
        
        return "redirect:/admin/review";
    }

    /**
     * 리뷰 업데이트 (내용, 평점, 상태)
     */
    @PostMapping("/{reviewId}/update")
    public String updateReview(
            @PathVariable Long reviewId,
            @RequestParam String content,
            @RequestParam Integer rating,
            @RequestParam String commentStatus,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "search", required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.updateReview(reviewId, content, rating, commentStatus);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("리뷰 업데이트 실패: reviewId={}, error={}", reviewId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 수정 중 오류가 발생했습니다.");
        }
        
        // 페이지 파라미터 유지
        if (page != null && page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
        if (searchType != null && !searchType.isEmpty()) {
            redirectAttributes.addAttribute("searchType", searchType);
        }
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
        }
        
        return "redirect:/admin/review";
    }
}

