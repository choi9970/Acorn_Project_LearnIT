package com.learnit.learnit.mypage.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MyQnADTO {
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
    private Long answerUserId; // 답변 작성자 ID (관리자 여부 확인용)
    private Long firstChapterId; // 첫 번째 챕터 ID (서비스에서 설정)
}

