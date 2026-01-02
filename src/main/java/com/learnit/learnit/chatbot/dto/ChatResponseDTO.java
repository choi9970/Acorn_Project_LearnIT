package com.learnit.learnit.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spring → JS 챗봇 위젯 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    private String reply;
    private String sessionId;

    // ✅ 추가: 강의 카드 목록
    private List<AgentChatResponseDTO.CourseItemDTO> items;
}