package com.learnit.learnit.mypage.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QnADTO {
    private Long qnaId;
    private Integer courseId;
    private String courseTitle;
    private Long userId;
    private String userName;
    private String content;
    private String isResolved;
    private LocalDateTime createdAt;
    private String answer; // 답변 내용 (qna_answer 테이블에서 조회)
    private LocalDateTime answeredAt; // 답변 작성일 (qna_answer 테이블에서 조회)
}

