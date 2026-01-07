package com.learnit.learnit.admin.qna;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQnaService {

    private final AdminQnaRepository repo;

    public int getTotalCount(String type, String status, String searchField, String search, Integer searchQnaId) {
        return repo.countQnas(type, status, searchField, search, searchQnaId);
    }

    public List<AdminQnaDto> getList(String type, String status, String searchField, String search,
                                     Integer searchQnaId, int offset, int size) {
        return repo.selectQnas(offset, size, type, status, searchField, search, searchQnaId);
    }

    public AdminQnaDto getDetail(int qnaId) {
        return repo.selectQnaDetail(qnaId);
    }

    @Transactional
    public void saveAnswerAndStatus(int qnaId, long adminUserId, String content, String newStatus) {
        Integer answerId = repo.selectLatestAnswerId(qnaId);

        // ✅ 답변 업서트
        if (content != null) {
            String trimmed = content.trim();
            if (!trimmed.isEmpty()) {
                if (answerId == null) repo.insertAnswer(qnaId, adminUserId, trimmed);
                else repo.updateAnswer(answerId, trimmed);
            }
        }

        // ✅ 상태 반영 (PASS => Y, ACTIVE => N)
        if ("PASS".equalsIgnoreCase(newStatus)) repo.updateResolved(qnaId, "Y");
        else if ("ACTIVE".equalsIgnoreCase(newStatus)) repo.updateResolved(qnaId, "N");
    }

    @Transactional
    public void delete(int qnaId) {
        repo.softDeleteAnswersByQnaId(qnaId);
        repo.softDeleteQuestionById(qnaId);
    }
}
