package com.learnit.learnit.qna.service;

import com.learnit.learnit.qna.dto.CourseQnaDto;
import com.learnit.learnit.qna.mapper.CourseQnaMapper;
import com.learnit.learnit.user.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("courseQnaService")
@RequiredArgsConstructor
public class CourseQnaService {

    private final CourseQnaMapper courseQnaMapper;

    private boolean isEnrolledActive(Long userId, Long courseId) {
        String status = courseQnaMapper.selectEnrollmentStatus(userId, courseId);
        return "ACTIVE".equals(status);
    }

    private boolean isAdmin(Long userId) {
        return courseQnaMapper.isAdmin(userId) > 0;
    }

    private boolean isSubAdminOfCourse(Long userId, Long courseId) {
        return courseQnaMapper.isSubAdminOfCourse(userId, courseId) > 0;
    }

    private boolean canAnswerAsAdmin(Long userId, Long courseId) {
        return isAdmin(userId) || isSubAdminOfCourse(userId, courseId);
    }

    @Transactional(readOnly = true)
    public List<CourseQnaDto.QuestionRes> getQuestions(Long courseId) {
        Long loginUserId = SessionUtils.getLoginUserId();
        boolean isAdmin = (loginUserId != null && isAdmin(loginUserId));
        boolean isSubAdmin = (loginUserId != null && isSubAdminOfCourse(loginUserId, courseId));
        boolean canAdminAnswer = (loginUserId != null && (isAdmin || isSubAdmin));

        List<CourseQnaDto.QuestionRes> questions = courseQnaMapper.selectQuestions(courseId);
        List<CourseQnaDto.AnswerRes> answers = courseQnaMapper.selectAnswers(courseId);

        Map<Long, List<CourseQnaDto.AnswerRes>> byQna = answers.stream()
                .collect(Collectors.groupingBy(CourseQnaDto.AnswerRes::getQnaId));

        for (CourseQnaDto.QuestionRes q : questions) {
            Long questionOwnerId = q.getUserId();
            boolean isQuestionOwner = (loginUserId != null && Objects.equals(loginUserId, questionOwnerId));

            // 질문 수정: 작성자만
            q.setCanEditQuestion(isQuestionOwner);

            // 질문 삭제: 작성자 또는 관리자(전체/강의)
            q.setCanDeleteQuestion(isQuestionOwner || isAdmin || isSubAdmin);

            // 관리자 답변 버튼
            q.setCanAnswer(canAdminAnswer);

            // ✅ 질문자 댓글 버튼 (원글에만 댓글)
            q.setCanComment(isQuestionOwner);

            q.setCanSetResolved(canAdminAnswer);

            List<CourseQnaDto.AnswerRes> list = byQna.getOrDefault(q.getQnaId(), Collections.emptyList());

            for (CourseQnaDto.AnswerRes a : list) {
                boolean isWriter = (loginUserId != null && Objects.equals(loginUserId, a.getUserId()));

                // 수정: 작성자만
                a.setCanEdit(isWriter);

                // 삭제: 작성자 본인 OR 관리자(전체/강의)
                a.setCanDelete(loginUserId != null && (isWriter || isAdmin || isSubAdmin));

                // ✅ 답변에 댓글 다는 기능 자체 제거 (항상 false)
                a.setCanReply(false);

                // ✅ parentAnswerId는 사용하지 않음 (항상 null로 운영)
                // 댓글/답변 구분 플래그(프론트 배지용)
                // - 관리자(전체/강의) 작성이면 답변, 질문자 작성이면 댓글
                boolean isComment = (a.getParentAnswerId() == null) && Objects.equals(a.getUserId(), questionOwnerId);
                a.setComment(isComment);

                // children 미사용
                a.setChildren(Collections.emptyList());
            }

            // 보기 좋게: 답변/댓글 최신순 또는 원하면 다른 정렬로
            // (selectAnswers 쿼리의 ORDER BY가 있다면 그대로 따라감)
            q.setAnswers(list);
        }

        return questions;
    }

    @Transactional
    public void createQuestion(Long courseId, String content) {
        Long userId = SessionUtils.requireLoginUserId();

        if (!isEnrolledActive(userId, courseId)) {
            throw new SecurityException("수강중인 회원만 질문 가능");
        }
        courseQnaMapper.insertQuestion(courseId, userId, content);
    }

    @Transactional
    public void updateQuestion(Long qnaId, String content) {
        Long userId = SessionUtils.requireLoginUserId();

        int updated = courseQnaMapper.updateQuestionByOwner(qnaId, userId, content);
        if (updated == 0) {
            throw new SecurityException("본인 질문만 수정 가능");
        }
    }

    @Transactional
    public void deleteQuestion(Long qnaId, Long courseId) {
        Long userId = SessionUtils.requireLoginUserId();

        Long ownerId = courseQnaMapper.selectQuestionOwnerId(qnaId);
        boolean allow = Objects.equals(userId, ownerId) || isAdmin(userId) || isSubAdminOfCourse(userId, courseId);

        if (!allow) throw new SecurityException("삭제 권한 없음");

        courseQnaMapper.softDeleteQuestion(qnaId);
    }

    /**
     * ✅ 이제 createAnswer는 "질문(원글)에 다는 답변/댓글"만 허용
     * - parentAnswerId는 항상 null이어야 함 (답변에 댓글 금지)
     * - 관리자(전체/강의): 답변 가능 + 해결여부 변경 가능
     * - 질문자: 댓글 가능(해결여부 변경 불가)
     */
    @Transactional
    public void createAnswer(CourseQnaDto.AnswerCreateReq req, Long courseId) {
        Long userId = SessionUtils.requireLoginUserId();

        // ❌ 답변에 대한 댓글(대댓글) 기능 제거: parentAnswerId가 오면 차단
        if (req.getParentAnswerId() != null) {
            throw new SecurityException("답변에 댓글을 달 수 없습니다. 질문에만 댓글을 달 수 있어요.");
        }

        Long questionOwnerId = courseQnaMapper.selectQuestionOwnerId(req.getQnaId());
        boolean isQuestionOwner = Objects.equals(userId, questionOwnerId);
        boolean isAdminOrSub = canAnswerAsAdmin(userId, courseId);

        // 관리자 또는 질문자만 작성 가능
        if (!isAdminOrSub && !isQuestionOwner) {
            throw new SecurityException("작성 권한 없음");
        }

        // insert (parentAnswerId는 null)
        courseQnaMapper.insertAnswer(req.getQnaId(), userId, null, req.getContent());

        // 관리자 답변이면 해결여부 반영 가능
        if (isAdminOrSub && req.getIsResolved() != null) {
            courseQnaMapper.updateQuestionResolved(req.getQnaId(), req.getIsResolved());
        }

        // 질문자가 댓글을 달면 미해결 처리(원하면 이 라인 제거 가능)
        if (isQuestionOwner) {
            courseQnaMapper.updateQuestionResolved(req.getQnaId(), "N");
        }
    }

    @Transactional
    public void updateAnswer(Long answerId, CourseQnaDto.AnswerUpdateReq req, Long courseId) {
        Long userId = SessionUtils.requireLoginUserId();

        Long ownerId = courseQnaMapper.selectAnswerOwnerId(answerId);
        boolean isWriter = Objects.equals(userId, ownerId);

        if (!isWriter && !canAnswerAsAdmin(userId, courseId)) throw new SecurityException("수정 권한 없음");

        courseQnaMapper.updateAnswer(answerId, req.getContent());

        // 해결여부 변경은 관리자만 의미있게 처리 (프론트에서도 관리자 UI에서만 보이게)
        if (req.getIsResolved() != null && canAnswerAsAdmin(userId, courseId)) {
            Long qnaId = courseQnaMapper.selectAnswerQnaId(answerId);
            courseQnaMapper.updateQuestionResolved(qnaId, req.getIsResolved());
        }
    }

    @Transactional
    public void deleteAnswer(Long answerId, Long courseId) {
        Long userId = SessionUtils.requireLoginUserId();

        Long ownerId = courseQnaMapper.selectAnswerOwnerId(answerId);
        boolean isWriter = Objects.equals(userId, ownerId);

        // 작성자 본인 OR 관리자(전체/강의)
        if (!isWriter && !canAnswerAsAdmin(userId, courseId)) throw new SecurityException("삭제 권한 없음");

        courseQnaMapper.softDeleteAnswer(answerId);
    }
}
