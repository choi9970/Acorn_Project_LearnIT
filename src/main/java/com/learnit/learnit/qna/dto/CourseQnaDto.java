package com.learnit.learnit.qna.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Course QNA DTO
 * - 질문(Question) + 답변/댓글(Answer)
 * - 현재 요구사항: "원글(질문)에만 댓글" 구조를 위해
 *   ✅ QuestionRes.canComment 추가
 *   ✅ AnswerRes.isComment 추가
 */
public class CourseQnaDto {

    // =========================================================
    // ✅ 질문 목록 응답 DTO
    // =========================================================
    @Data
    public static class QuestionRes {
        private Long qnaId;
        private Long courseId;
        private Long userId;

        private String writerNickname;
        private String content;
        private String createdAt;

        // 해결여부 (Y/N)
        private String isResolved;

        // -------------------------
        // 권한/상태 플래그
        // -------------------------
        // 질문 수정 가능(작성자)
        private boolean canEditQuestion;

        // 질문 삭제 가능(작성자 또는 관리자)
        private boolean canDeleteQuestion;

        // 관리자 답변 가능(전체/강의 관리자)
        private boolean canAnswer;

        // ✅ 질문자 댓글 가능(원글에만 댓글)
        private boolean canComment;

        // 관리자 해결여부 변경 가능
        private boolean canSetResolved;

        // -------------------------
        // 답변/댓글 리스트
        // -------------------------
        private List<AnswerRes> answers = new ArrayList<>();
    }

    // =========================================================
    // ✅ 답변/댓글 응답 DTO
    // =========================================================
    @Data
    public static class AnswerRes {
        private Long answerId;
        private Long qnaId;
        private Long userId;


        // (기존 구조 호환) 답변에 대한 대댓글을 쓰던 구조에서 사용
        // 현재 "원글에만 댓글" 구조에서는 항상 null로 운용 권장
        private Long parentAnswerId;

        private String writerNickname;
        private String content;
        private String createdAt;

        // -------------------------
        // 권한 플래그
        // -------------------------
        private boolean canEdit;
        private boolean canDelete;

        // (기존 구조 호환) 답변에 대댓글 가능 여부
        // 현재 구조에서는 false로 내려주면 됨
        private boolean canReply;

        // ✅ "답변/댓글 구분" (프론트 배지/스타일링용)
        // true  = 댓글(질문자 작성)
        // false = 답변(관리자 작성)
        private boolean isComment;

        // (기존 구조 호환) 대댓글 트리
        // 현재 구조에서는 사용하지 않으므로 빈 리스트로 내려주면 됨
        private List<AnswerRes> children = new ArrayList<>();


    }

    // =========================================================
    // ✅ 답변/댓글 생성 요청 DTO
    // =========================================================
    @Data
    public static class AnswerCreateReq {
        private Long qnaId;

        // "답변에 댓글(대댓글)" 구조일 때 사용
        // 현재 "원글(질문)에만 댓글" 구조에서는 항상 null이어야 함
        private Long parentAnswerId;

        private String content;

        // 관리자 답변일 때만 사용 (해결여부 변경)
        // 댓글 작성 시에는 null로 보내는 것을 권장
        private String isResolved;
    }

    // =========================================================
    // ✅ 답변/댓글 수정 요청 DTO
    // =========================================================
    @Data
    public static class AnswerUpdateReq {
        private String content;

        // 관리자 답변 수정 시 해결여부 변경 가능
        // 댓글 수정 시에는 null로 보내는 것을 권장
        private String isResolved;
    }

    // =========================================================
    // ✅ 질문 생성/수정 요청 DTO (있으면 편함)
    // =========================================================
    @Data
    public static class QuestionCreateReq {
        private Long courseId;
        private String content;
    }

    @Data
    public static class QuestionUpdateReq {
        private String content;
    }
}
