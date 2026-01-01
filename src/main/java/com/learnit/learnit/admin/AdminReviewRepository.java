package com.learnit.learnit.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ReviewMapper.xml 호출 DAO 계층
 */
@Mapper
public interface AdminReviewRepository {
    /**
     * 리뷰 목록 조회 (페이징, 검색, 필터링)
     */
    List<AdminReviewDto> selectReviews(
        @Param("offset") int offset,
        @Param("limit") int limit,
        @Param("status") String status,
        @Param("searchKeyword") String searchKeyword
    );

    /**
     * 리뷰 총 개수 조회
     */
    int countReviews(
        @Param("status") String status,
        @Param("searchKeyword") String searchKeyword
    );

    /**
     * 리뷰 상세 조회
     */
    AdminReviewDto selectReviewById(@Param("reviewId") Long reviewId);

    /**
     * 리뷰 승인
     */
    void approveReview(@Param("reviewId") Long reviewId);

    /**
     * 리뷰 거부
     */
    void rejectReview(@Param("reviewId") Long reviewId);

    /**
     * 리뷰 상태 변경
     */
    void updateStatus(@Param("reviewId") Long reviewId, @Param("status") String status);

    /**
     * 리뷰 삭제
     */
    void deleteReview(@Param("reviewId") Long reviewId);
}

