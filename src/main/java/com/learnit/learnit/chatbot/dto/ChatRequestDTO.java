package com.learnit.learnit.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JS 챗봇 위젯 → Spring 요청 DTO
 * POST /api/chatbot/chat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {
    private String message;
    private String sessionId; // null 가능
}