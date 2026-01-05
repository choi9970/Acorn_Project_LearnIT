package com.learnit.learnit.admin.qna;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQnaService {

    private final AdminQnaRepository repo;

    public List<AdminQnaDto> getQnas(int page, int size, String type, String status, String search) {
        int offset = (page - 1) * size;
        return repo.selectQnas(offset, size, type, status, search);
    }

    public int getTotalCount(String type, String status, String search) {
        return repo.countQnas(type, status, search);
    }

    public AdminQnaDto getDetail(int qnaId) {
        return repo.selectQnaDetail(qnaId);
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
