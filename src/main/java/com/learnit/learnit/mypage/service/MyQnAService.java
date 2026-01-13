package com.learnit.learnit.mypage.service;

import com.learnit.learnit.courseVideo.service.CourseVideoService;
import com.learnit.learnit.mypage.dto.MyQnADTO;
import com.learnit.learnit.mypage.mapper.MyQnAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyQnAService {

    private final MyQnAMapper qnAMapper;
    private final CourseVideoService courseVideoService;

    /**
     * 사용자가 작성한 Q&A 목록 조회 (페이징)
     * 관리자/서브어드민인 경우 분기 처리
     */
    public List<MyQnADTO> getMyQnAList(Long userId, String userRole, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }
        int offset = (page - 1) * size;
        List<MyQnADTO> qnaList;

        // 관리자(ADMIN)인 경우: 모든 Q&A 조회
        if ("ADMIN".equals(userRole)) {
            qnaList = qnAMapper.selectAdminQnAList(offset, size);
        }
        // 서브 어드민(SUB_ADMIN)인 경우: 관리하는 강의의 Q&A만 조회
        else if ("SUB_ADMIN".equals(userRole)) {
            List<Integer> managedCourseIds = qnAMapper.selectManagedCourseIds(userId);
            if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                // 관리하는 강의가 없으면 빈 리스트 반환
                return List.of();
            }
            qnaList = qnAMapper.selectSubAdminQnAList(managedCourseIds, offset, size);
        }
        // 일반 사용자인 경우: 본인이 작성한 Q&A만 조회
        else {
            qnaList = qnAMapper.selectMyQnAList(userId, offset, size);
        }
        
        // 각 Q&A에 대해 첫 번째 챕터 ID 조회 및 설정
        for (MyQnADTO qna : qnaList) {
            if (qna.getCourseId() != null && qna.getCourseId() > 0) {
                // 강의 영상 페이지로 이동하기 위해 첫 번째 챕터 ID를 반드시 조회
                Long firstChapterId = courseVideoService.getFirstChapterId(qna.getCourseId().longValue());
                qna.setFirstChapterId(firstChapterId);
                // firstChapterId가 null이면 강의 영상 페이지로 갈 수 없으므로, 
                // null인 경우에도 일단 설정 (강의에 챕터가 없는 경우는 드뭅지만 가능)
            }
            
            // 일반 사용자는 강의 관련 질문의 답변 내용을 볼 수 없도록 설정 (강의 영상 페이지에서만 확인 가능)
            // 단, 챗봇 질문(course_id가 NULL)의 답변은 마이 Q&A에서 바로 볼 수 있게 함
            if (!"ADMIN".equals(userRole) && !"SUB_ADMIN".equals(userRole)) {
                // 강의 관련 질문(course_id가 있는 경우)의 답변은 숨김
                if (qna.getCourseId() != null && qna.getCourseId() > 0) {
                    // 답변이 있으면 "답변 등록되었습니다"만 알 수 있도록 answer 내용은 숨김
                    if (qna.getAnswer() != null && !qna.getAnswer().isEmpty()) {
                        qna.setAnswer(""); // 답변 내용은 숨기지만 answeredAt은 유지하여 답변 등록 여부는 알 수 있음
                    }
                }
                // 챗봇 질문(course_id가 NULL)의 답변은 그대로 표시
            }
        }
        
        return qnaList;
    }

    /**
     * 사용자가 작성한 Q&A 총 개수 조회
     * 관리자/서브어드민인 경우 분기 처리
     */
    public int getMyQnACount(Long userId, String userRole) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        // 관리자(ADMIN)인 경우: 모든 Q&A 개수
        if ("ADMIN".equals(userRole)) {
            return qnAMapper.countAdminQnAList();
        }
        // 서브 어드민(SUB_ADMIN)인 경우: 관리하는 강의의 Q&A 개수
        else if ("SUB_ADMIN".equals(userRole)) {
            List<Integer> managedCourseIds = qnAMapper.selectManagedCourseIds(userId);
            if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                return 0;
            }
            return qnAMapper.countSubAdminQnAList(managedCourseIds);
        }
        // 일반 사용자인 경우: 본인이 작성한 Q&A 개수
        else {
            return qnAMapper.countMyQnAList(userId);
        }
    }
}
