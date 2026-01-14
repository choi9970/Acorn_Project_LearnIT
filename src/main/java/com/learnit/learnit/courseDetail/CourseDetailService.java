package com.learnit.learnit.courseDetail;

import com.learnit.learnit.course.CourseDTO;
import com.learnit.learnit.enroll.EnrollmentMapper;
import com.learnit.learnit.enroll.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseDetailService {

    private final CourseDetailMapper courseDetailMapper;
    private final EnrollmentMapper enrollmentMapper;

    private final CourseDetailRepository courseDetailRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseDTO getCourse(int courseId) {
        return courseDetailMapper.selectCourseDetail(courseId);
    }

    public boolean isEnrolled(Long userId, int courseId) {
        return enrollmentMapper.countEnrollment(userId, courseId) > 0;
    }

    public List<ChapterDTO> getChapters(int courseId) {
        List<ChapterDTO> list = courseDetailMapper.selectChaptersByCourseId(courseId);
        return (list == null) ? Collections.emptyList() : list;
    }

    public Map<String, List<ChapterDTO>> getCurriculumSectionMap(int courseId) {
        List<ChapterDTO> chapters = getChapters(courseId);
        if (chapters.isEmpty()) return Collections.emptyMap();

        Map<String, List<ChapterDTO>> sectionMap = new LinkedHashMap<>();
        for (ChapterDTO ch : chapters) {
            String key = ch.getSectionTitle();
            if (key == null || key.isBlank()) key = "섹션 1. 커리큘럼";
            sectionMap.computeIfAbsent(key, k -> new ArrayList<>()).add(ch);
        }
        return sectionMap;
    }

    // ✅ 추가: 화면용 값들 (DTO에 안 넣고 model로 주입)
    public String getInstructorNameByUserId(Integer userId) {
        if (userId == null) return null;
        return courseDetailMapper.selectInstructorNameByUserId(userId);
    }

    public String getPeriodTextByCourseId(int courseId) {
        return courseDetailMapper.selectPeriodTextByCourseId(courseId);
    }

    public String getCategoryNameByCategoryId(Integer categoryId) {
        if (categoryId == null) return null;
        return courseDetailMapper.selectCategoryNameByCategoryId(categoryId);
    }

    /**
     * comment_status 값이 REJECTED(대소문자 혼재 포함)인지 여부
     * - 운영 중 상태값이 'Rejected'로 들어오는 케이스 방어
     */
    private boolean isRejectedStatus(String status) {
        return status != null && status.trim().equalsIgnoreCase("REJECTED");
    }

    public List<ReviewDTO> getReviewsByCourseId(Long courseId) {
        List<ReviewDTO> list = courseDetailRepository.findByCourseIdAndDeleteFlg(courseId, 0);

        // comment_status가 null 이거나 'Rejected/REJECTED'가 아닌 것만 노출
        return list.stream()
                .filter(r -> r.getCommentStatus() == null || !isRejectedStatus(r.getCommentStatus()))
                .toList();
    }

    public Map<String, Object> getReviewSummary(Long courseId) {

        List<ReviewDTO> list = courseDetailRepository.findByCourseIdAndDeleteFlg(courseId, 0);

        List<ReviewDTO> filtered = list.stream()
                .filter(r -> r.getCommentStatus() == null || !isRejectedStatus(r.getCommentStatus()))
                .toList();

        double average = filtered.isEmpty()
                ? 0.0
                : filtered.stream()
                .mapToInt(ReviewDTO::getRating)
                .average()
                .orElse(0.0);

        Map<String, Object> result = new HashMap<>();
        result.put("average", average);
        result.put("count", filtered.size());

        return result;
    }

    @Transactional
    public ReviewDTO createReview(Long courseId, Long userId, ReviewDTO input) {

        // 0. 수강 여부 체크
        boolean enrolled = enrollmentRepository
                .existsByUserIdAndCourseIdAndStatus(userId, courseId, "ACTIVE");

        if (!enrolled) {
            throw new IllegalStateException("수강 중인 사용자만 리뷰를 작성할 수 있습니다.");
        }

        // 1. 이 유저가 이 강의에 쓴 리뷰가 있는지 (삭제 포함 전체 조회)
        Optional<ReviewDTO> optional = courseDetailRepository.findByCourseIdAndUserId(courseId, userId);

        if (optional.isPresent()) {
            ReviewDTO existing = optional.get();

            // 현재 "살아 있는" 리뷰인지 판별
            boolean isActive =
                    (existing.getDeleteFlg() == null || existing.getDeleteFlg() == 0)
                            && !isRejectedStatus(existing.getCommentStatus());

            if (isActive) {
                throw new IllegalStateException("이미 이 강의에 수강평이 등록되었습니다.");
            }

            // 삭제되었거나(REJECTED) 숨겨진 리뷰가 있는 상태 → 같은 row 재활성화
            existing.setRating(input.getRating());
            existing.setContent(input.getContent());
            existing.setDeleteFlg(0);
            existing.setCommentStatus("VISIBLE");

            return courseDetailRepository.save(existing);
        }

        ReviewDTO review = ReviewDTO.builder()
                .courseId(courseId)
                .userId(userId)
                .rating(input.getRating())
                .content(input.getContent())
                .deleteFlg(0)
                .commentStatus("VISIBLE")
                .build();

        return courseDetailRepository.save(review);
    }

    @Transactional
    public ReviewDTO updateReview(Long reviewId, Long userId, ReviewDTO input) {

        ReviewDTO review = courseDetailRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getUserId().equals(userId)) {
            throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        review.setRating(input.getRating());
        review.setContent(input.getContent());

        return courseDetailRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {

        ReviewDTO review = courseDetailRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getUserId().equals(userId)) {
            throw new IllegalStateException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        review.setDeleteFlg(1);
        courseDetailRepository.save(review);
    }

    /**
     * 해당 userId가 courseId 강의를 수강 중인지 여부
     */
    @Transactional(readOnly = true)
    public boolean isEnrolledUser(Long courseId, Long userId) {
        if (userId == null) return false;

        return enrollmentRepository
                .existsByUserIdAndCourseIdAndStatus(userId, courseId, "ACTIVE");
    }

    public boolean canWriteReview(Long courseId, Long userId) {

        // 1️⃣ 수강 여부
        if (!isEnrolledUser(courseId, userId)) {
            return false;
        }

        // 2️⃣ delete_flg = 0 인 내 리뷰 존재 여부
        int activeReviewCount =
                courseDetailMapper.countActiveReviewByCourseAndUser(courseId, userId);

        // 3️⃣ 활성 리뷰 없으면 작성 가능
        return activeReviewCount == 0;
    }

    public Long getLastWatchedChapterId(Long userId, int courseId) {
        Long lastChapterId = courseDetailMapper.selectLastWatchedChapterId(userId, courseId);
        if (lastChapterId != null) {
            return lastChapterId;
        }

        List<ChapterDTO> chapters = getChapters(courseId);
        if (!chapters.isEmpty()) {
            return chapters.get(0).getChapterId();
        }
        return null;
    }
}
