package com.learnit.learnit.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JS 챗봇 위젯 → Spring 요청 DTO
 * POST /api/chatbot/qna
 *
 * title 컬럼은 사용하지 않으므로 받지 않음.
 * (JS가 title을 보내도 Jackson이 모르는 필드는 무시하도록 설정되어 있으면 무관)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QnaCreateRequestDTO {
    private Integer  courseId;
    private String content;
}
