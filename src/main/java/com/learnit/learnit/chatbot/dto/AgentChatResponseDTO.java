package com.learnit.learnit.chatbot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Python(chat-agent) → Spring 응답 DTO
 */
@Data
@NoArgsConstructor
public class AgentChatResponseDTO {
    private String reply;
    private String sessionId;

    // ✅ 추가: 강의 목록(카드 렌더링용)
    private List<CourseItemDTO> items;

    @Data
    @NoArgsConstructor
    public static class CourseItemDTO {
        private Integer courseId;
        private String title;
        private String description;
        private Integer price;
        private String detailUrl;
    }
}