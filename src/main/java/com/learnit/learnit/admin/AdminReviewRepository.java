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
        @Param("searchType") String searchType,
        @Param("searchKeyword") String searchKeyword,
        @Param("managedCourseIds") List<Integer> managedCourseIds
    );

    /**
     * 리뷰 총 개수 조회
     */
    int countReviews(
        @Param("searchType") String searchType,
        @Param("searchKeyword") String searchKeyword,
        @Param("managedCourseIds") List<Integer> managedCourseIds
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

    /**
     * 사용자가 전체 권한(course_id가 NULL)을 가지고 있는지 확인
     */
    boolean hasFullAdminAccess(@Param("userId") Long userId);

    /**
     * 사용자가 관리하는 강의 ID 목록 조회 (서브 어드민 필터링용)
     */
    List<Integer> selectManagedCourseIds(@Param("userId") Long userId);

    /**
     * 리뷰 업데이트 (내용, 평점, 상태)
     */
    void updateReview(
        @Param("reviewId") Long reviewId,
        @Param("content") String content,
        @Param("rating") Integer rating,
        @Param("commentStatus") String commentStatus
    );
}

