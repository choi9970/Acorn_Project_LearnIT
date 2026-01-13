package com.learnit.learnit.admin.qna;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminQnaDTO {
    // Q&A 기본 정보
    private Integer qnaId;
    private Integer courseId;
    private String courseTitle;

    // 질문 정보(원 질문)
    private Long questionUserId;
    private String questionUserName;
    private String questionUserEmail;
    private String questionTitle;
    private String questionContent;
    private LocalDateTime questionCreatedAt;
    private String isResolved;

    // ✅ 화면 표시용 제목(제목 없으면 질문 내용 요약)
    private String questionTitleDisplay;

    // ✅ 화면 표시용 질문(최신 학생 글 반영)
    private String questionContentDisplay;

    // ✅ 최신 "학생(비-스태프)" 글 1개(원글/댓글 느낌으로 새로 달린 글)
    private String latestStudentContent;
    private LocalDateTime latestStudentCreatedAt;

    // 답변 정보(관리자/서브관리자 최신 답변 1개)
    private Integer answerId;
    private Long answerUserId;
    private String answerUserName;
    private String answerContent;
    private LocalDateTime answerCreatedAt;

    // 화면 표시용
    private String typeLabel;   // "강의 Q&A" 또는 "전체 Q&A"
    private String statusLabel; // "ACTIVE" 또는 "PASS"
}
