package com.learnit.learnit.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spring → Python(chat-agent) 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRequestDTO {
    private String message;
    private String sessionId; // null 가능
    private String userId;    // 로그인 아니면 "anonymous"
}