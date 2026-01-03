package com.learnit.learnit.chatbot.controller;
import com.learnit.learnit.chatbot.dto.ChatRequestDTO;
import com.learnit.learnit.chatbot.dto.ChatResponseDTO;
import com.learnit.learnit.chatbot.dto.QnaCreateRequestDTO;
import com.learnit.learnit.chatbot.service.ChatbotService;
import com.learnit.learnit.chatbot.service.QnaService;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * JS 챗봇 위젯에서 호출하는 REST API
 * - POST /api/chatbot/chat : 강의 추천(파이썬 chat-agent 연동)
 * - POST /api/chatbot/qna  : 상담원 문의 DB 저장
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotApiController {

    private final ChatbotService chatbotService;
    private final QnaService qnaService;

    @PostMapping("/chat")
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO req, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        String userKey = (userId != null) ? String.valueOf(userId) : "anonymous";

        return chatbotService.recommend(req.getMessage(), req.getSessionId(), userKey);
    }

    @PostMapping("/qna")
    public ResponseEntity<Void> createQna(@RequestBody QnaCreateRequestDTO req, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        qnaService.createQna(req.getCourseId(), userId, req.getContent());
        return ResponseEntity.ok().build();
    }
}