package com.learnit.learnit.admin.qna;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQnaService {

    private final AdminQnaRepository repo;

    public int getTotalCount(String type, String status, String searchField, String search, Integer searchQnaId, Long instructorUserId) {
        return repo.countQnas(type, status, searchField, search, searchQnaId, instructorUserId);
    }

    public List<AdminQnaDto> getList(String type, String status, String searchField, String search,
                                     Integer searchQnaId, int offset, int size, Long instructorUserId) {
        return repo.selectQnas(offset, size, type, status, searchField, search, searchQnaId, instructorUserId);
    }

    public AdminQnaDto getDetail(int qnaId) {
        return repo.selectQnaDetail(qnaId);
    }

    public Long getInstructorUserIdByCourseId(Integer courseId) {
        if (courseId == null) return null;
        return repo.selectInstructorUserIdByCourseId(courseId);
    }

    @Transactional
    public void saveAnswerAndStatus(int qnaId, long adminUserId, String content, String newStatus) {
        Integer answerId = repo.selectLatestAnswerId(qnaId);

        if (content != null) {
            String trimmed = content.trim();
            if (!trimmed.isEmpty()) {
                if (answerId == null) repo.insertAnswer(qnaId, adminUserId, trimmed);
                else repo.updateAnswer(answerId, trimmed);
            }
        }

        if ("PASS".equalsIgnoreCase(newStatus)) repo.updateResolved(qnaId, "Y");
        else if ("ACTIVE".equalsIgnoreCase(newStatus)) repo.updateResolved(qnaId, "N");
    }

    @Transactional
    public void delete(int qnaId) {
        repo.softDeleteAnswersByQnaId(qnaId);
        repo.softDeleteQuestionById(qnaId);
    }
}
