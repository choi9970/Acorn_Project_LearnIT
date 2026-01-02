package com.learnit.learnit.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 리뷰 조회, comment_status 변경, 삭제 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final AdminReviewRepository adminReviewRepository;

    /**
     * 리뷰 목록 조회 (페이징)
     */
    public List<AdminReviewDto> getReviews(int page, int size, String status, String searchKeyword) {
        try {
            int offset = (page - 1) * size;
            int limit = size;
            List<AdminReviewDto> reviews = adminReviewRepository.selectReviews(offset, limit, status, searchKeyword);
            log.info("리뷰 목록 조회 성공: page={}, size={}, status={}, searchKeyword={}, count={}", 
                page, size, status, searchKeyword, reviews != null ? reviews.size() : 0);
            return reviews != null ? reviews : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("리뷰 목록 조회 실패: page={}, size={}, status={}, searchKeyword={}, error={}", 
                page, size, status, searchKeyword, e.getMessage(), e);
            // 테이블이 없거나 에러 발생 시 빈 리스트 반환
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 리뷰 총 개수 조회
     */
    public int getReviewCount(String status, String searchKeyword) {
        try {
            int count = adminReviewRepository.countReviews(status, searchKeyword);
            log.info("리뷰 총 개수 조회 성공: status={}, searchKeyword={}, count={}", status, searchKeyword, count);
            return count;
        } catch (Exception e) {
            log.error("리뷰 총 개수 조회 실패: status={}, searchKeyword={}, error={}", 
                status, searchKeyword, e.getMessage(), e);
            // 테이블이 없거나 에러 발생 시 0 반환
            return 0;
        }
    }

    /**
     * 리뷰 상세 조회
     */
    public AdminReviewDto getReview(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 없습니다.");
        }
        return adminReviewRepository.selectReviewById(reviewId);
    }

    /**
     * 리뷰 승인
     */
    @Transactional
    public void approveReview(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 없습니다.");
        }
        adminReviewRepository.approveReview(reviewId);
    }

    /**
     * 리뷰 거부
     */
    @Transactional
    public void rejectReview(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 없습니다.");
        }
        adminReviewRepository.rejectReview(reviewId);
    }

    /**
     * 리뷰 상태 변경
     */
    @Transactional
    public void updateStatus(Long reviewId, String status) {
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 없습니다.");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("상태가 없습니다.");
        }
        // 유효한 상태 값 검증
        if (!status.equals("Active") && !status.equals("Approved") && !status.equals("Rejected")) {
            throw new IllegalArgumentException("유효하지 않은 상태입니다.");
        }
        adminReviewRepository.updateStatus(reviewId, status);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 없습니다.");
        }
        adminReviewRepository.deleteReview(reviewId);
    }
}

