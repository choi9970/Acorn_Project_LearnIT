package com.learnit.learnit.admin.qna;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQnaService {

    private final AdminQnaRepository repo;

    public List<AdminQnaDto> getQnas(int page, int size, String type, String status, String search) {
        try {
            int offset = (page - 1) * size;
            List<AdminQnaDto> result = repo.selectQnas(offset, size, type, status, search);
            if (result != null && !result.isEmpty()) {
                log.debug("QnA 목록 조회 성공: 총 {}개, 첫 번째 항목 courseId={}, courseTitle={}", 
                    result.size(), 
                    result.get(0).getCourseId(), 
                    result.get(0).getCourseTitle());
            }
            return result != null ? result : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("QnA 목록 조회 실패: page={}, size={}, type={}, status={}, search={}, error={}", 
                page, size, type, status, search, e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    public int getTotalCount(String type, String status, String search) {
        try {
            return repo.countQnas(type, status, search);
        } catch (Exception e) {
            log.error("QnA 총 개수 조회 실패: type={}, status={}, search={}, error={}", 
                type, status, search, e.getMessage(), e);
            return 0;
        }
    }

    public AdminQnaDto getDetail(int qnaId) {
        try {
            return repo.selectQnaDetail(qnaId);
        } catch (Exception e) {
            log.error("QnA 상세 조회 실패: qnaId={}, error={}", qnaId, e.getMessage(), e);
            return null;
        }
    }

    @Transactional
    public void saveAnswer(int qnaId, int adminUserId, String content, boolean markResolved) {
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("답변 내용을 입력하세요.");

        Integer answerId = repo.selectLatestAnswerId(qnaId);

        if (answerId == null) {
            repo.insertAnswer(qnaId, adminUserId, content);
        } else {
            repo.updateAnswer(answerId, content);
        }

        if (markResolved) {
            repo.updateResolved(qnaId, "Y");
        }
    }

    @Transactional
    public void updateStatus(int qnaId, String uiStatus) {
        if (uiStatus == null || uiStatus.isBlank())
            throw new IllegalArgumentException("상태값이 없습니다.");

        String isResolved = "PASS".equalsIgnoreCase(uiStatus) ? "Y" : "N";
        repo.updateResolved(qnaId, isResolved);
    }

    // ✅✅ Q&A 삭제 + AUTO_INCREMENT를 MAX+1로 재설정
    @Transactional
    public void deleteQna(int qnaId) {
        repo.deleteAnswersByQnaId(qnaId);
        repo.deleteQuestionById(qnaId);

        resetAutoIncrementToMaxPlusOne(); // ✅ 추가
    }

    private void resetAutoIncrementToMaxPlusOne() {
        int nextId = repo.selectNextQnaId();       // MAX+1 (비어있으면 1)
        repo.resetQnaAutoIncrement(nextId);        // AUTO_INCREMENT = nextId
    }
}
