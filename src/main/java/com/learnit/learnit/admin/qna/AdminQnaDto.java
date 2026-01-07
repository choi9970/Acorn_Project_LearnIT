package com.learnit.learnit.admin.qna;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminQnaDto {
    private Integer qnaId;

    private Integer courseId;
    private String courseTitle;

    private Integer questionUserId;
    private String questionUserName;
    private String questionUserEmail;

    private String questionTitle;
    private String questionContent;
    private LocalDateTime questionCreatedAt;

    private String isResolved; // 'Y'/'N'

    // 최신 답변(있으면)
    private Integer answerId;
    private Integer answerUserId;
    private String answerUserName;
    private String answerContent;
    private LocalDateTime answerCreatedAt;

    // 화면 표시용
    public String getTypeLabel() {
        return (courseId == null) ? "전체 Q&A" : "강의 Q&A";
    }
    public String getStatusLabel() {
        return "Y".equalsIgnoreCase(isResolved) ? "PASS" : "ACTIVE";
    }
}
