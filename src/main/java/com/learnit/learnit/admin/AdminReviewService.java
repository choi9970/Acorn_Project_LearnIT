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
     * 리뷰 목록 조회 (페이징, 서브 어드민 필터링)
     */
    public List<AdminReviewDto> getReviews(int page, int size, String searchType, String searchKeyword, Long userId) {
        try {
            int offset = (page - 1) * size;
            int limit = size;
            
            // 서브 어드민 필터링: 관리하는 course_id 목록 조회
            java.util.List<Integer> managedCourseIds = null;
            if (userId != null) {
                // 전체 권한(course_id가 NULL)이 있으면 필터링 없음
                boolean hasFullAccess = adminReviewRepository.hasFullAdminAccess(userId);
                if (!hasFullAccess) {
                    // 부분 권한만 있는 경우 해당 course_id 목록으로 필터링
                    managedCourseIds = adminReviewRepository.selectManagedCourseIds(userId);
                    if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                        // 권한이 없으면 빈 리스트 반환
                        return new java.util.ArrayList<>();
                    }
                }
            }
            
            List<AdminReviewDto> reviews = adminReviewRepository.selectReviews(offset, limit, searchType, searchKeyword, managedCourseIds);
            log.info("리뷰 목록 조회 성공: page={}, size={}, searchType={}, searchKeyword={}, userId={}, count={}", 
                page, size, searchType, searchKeyword, userId, reviews != null ? reviews.size() : 0);
            return reviews != null ? reviews : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("리뷰 목록 조회 실패: page={}, size={}, searchType={}, searchKeyword={}, userId={}, error={}", 
                page, size, searchType, searchKeyword, userId, e.getMessage(), e);
            // 테이블이 없거나 에러 발생 시 빈 리스트 반환
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 리뷰 총 개수 조회 (서브 어드민 필터링)
     */
    public int getReviewCount(String searchType, String searchKeyword, Long userId) {
        try {
            // 서브 어드민 필터링: 관리하는 course_id 목록 조회
            java.util.List<Integer> managedCourseIds = null;
            if (userId != null) {
                // 전체 권한(course_id가 NULL)이 있으면 필터링 없음
                boolean hasFullAccess = adminReviewRepository.hasFullAdminAccess(userId);
                if (!hasFullAccess) {
                    // 부분 권한만 있는 경우 해당 course_id 목록으로 필터링
                    managedCourseIds = adminReviewRepository.selectManagedCourseIds(userId);
                    if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                        // 권한이 없으면 0 반환
                        return 0;
                    }
                }
            }
            
            int count = adminReviewRepository.countReviews(searchType, searchKeyword, managedCourseIds);
            log.info("리뷰 총 개수 조회 성공: searchType={}, searchKeyword={}, userId={}, count={}", searchType, searchKeyword, userId, count);
            return count;
        } catch (Exception e) {
            log.error("리뷰 총 개수 조회 실패: searchType={}, searchKeyword={}, userId={}, error={}", 
                searchType, searchKeyword, userId, e.getMessage(), e);
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

    /**
     * 리뷰 업데이트 (내용, 평점, 상태)
     */
    @Transactional
    public void updateReview(Long reviewId, String content, Integer rating, String commentStatus) {
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 없습니다.");
        }
        if (content == null) {
            throw new IllegalArgumentException("리뷰 내용이 없습니다.");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이의 값이어야 합니다.");
        }
        if (commentStatus == null || commentStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("상태가 없습니다.");
        }
        // 유효한 상태 값 검증
        if (!commentStatus.equals("Active") && !commentStatus.equals("Approved") && !commentStatus.equals("Rejected")) {
            throw new IllegalArgumentException("유효하지 않은 상태입니다.");
        }
        adminReviewRepository.updateReview(reviewId, content, rating, commentStatus);
    }
}

