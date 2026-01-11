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
     */
    public List<MyQnADTO> getMyQnAList(Long userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }
        int offset = (page - 1) * size;
        List<MyQnADTO> qnaList = qnAMapper.selectMyQnAList(userId, offset, size);
        
        // 각 Q&A에 대해 첫 번째 챕터 ID 조회 및 설정
        for (MyQnADTO qna : qnaList) {
            if (qna.getCourseId() != null && qna.getCourseId() > 0) {
                // 강의 영상 페이지로 이동하기 위해 첫 번째 챕터 ID를 반드시 조회
                Long firstChapterId = courseVideoService.getFirstChapterId(qna.getCourseId().longValue());
                qna.setFirstChapterId(firstChapterId);
                // firstChapterId가 null이면 강의 영상 페이지로 갈 수 없으므로, 
                // null인 경우에도 일단 설정 (강의에 챕터가 없는 경우는 드뭅지만 가능)
            }
        }
        
        return qnaList;
    }

    /**
     * 사용자가 작성한 Q&A 총 개수 조회
     */
    public int getMyQnACount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }
        return qnAMapper.countMyQnAList(userId);
    }
}
