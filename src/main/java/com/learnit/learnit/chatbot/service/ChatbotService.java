package com.learnit.learnit.chatbot.service;

import com.learnit.learnit.chatbot.dto.AgentChatRequestDTO;
import com.learnit.learnit.chatbot.dto.AgentChatResponseDTO;
import com.learnit.learnit.chatbot.dto.ChatResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final RestTemplate restTemplate;

    public ChatbotService(@Qualifier("chatbotRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${chatbot.agent.base-url:http://localhost:8000}")
    private String agentBaseUrl;

    @Value("${chatbot.agent.chat-path:/api/chat}")
    private String agentChatPath;

    @Value("${chatbot.agent.retry.max-attempts:1}")
    private int maxAttempts;

    @Value("${chatbot.agent.retry.backoff-ms:300}")
    private long backoffMs;

    public ChatResponseDTO recommend(String message, String sessionId, String userId) {

        String safeSessionId = ensureSessionId(sessionId);
        String url = normalize(agentBaseUrl) + normalizePath(agentChatPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AgentChatRequestDTO payload = new AgentChatRequestDTO(message, safeSessionId, userId);
        HttpEntity<AgentChatRequestDTO> entity = new HttpEntity<>(payload, headers);

        int attempts = Math.max(1, maxAttempts);

        for (int i = 1; i <= attempts; i++) {
            long start = System.currentTimeMillis();
            try {
                AgentChatResponseDTO agentRes =
                        restTemplate.postForObject(url, entity, AgentChatResponseDTO.class);

                long took = System.currentTimeMillis() - start;
                log.info("[chat-agent] success ({} ms) url={}", took, url);

                if (agentRes == null) {
                    return new ChatResponseDTO(
                            "추천을 불러오는 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.",
                            safeSessionId,
                            null
                    );
                }

                String reply = (agentRes.getReply() != null && !agentRes.getReply().isBlank())
                        ? agentRes.getReply()
                        : "추천 결과를 가져왔어요!";

                String nextSessionId = (agentRes.getSessionId() != null && !agentRes.getSessionId().isBlank())
                        ? agentRes.getSessionId()
                        : safeSessionId;


                return new ChatResponseDTO(reply, nextSessionId, agentRes.getItems());

            } catch (RestClientException e) {
                long took = System.currentTimeMillis() - start;
                boolean retryable = isRetryable(e);

                log.warn("[chat-agent] failed ({} ms) attempt {}/{}. retryable={}, url={}",
                        took, i, attempts, retryable, url, e);

                if (!retryable || i == attempts) break;

                try {
                    Thread.sleep(Math.max(0, backoffMs));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return new ChatResponseDTO(
                "추천을 불러오는 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.",
                safeSessionId,
                null
        );
    }

    private boolean isRetryable(RestClientException e) {
        if (e instanceof ResourceAccessException) return true;

        if (e instanceof HttpStatusCodeException se) {
            HttpStatusCode code = se.getStatusCode();
            return code.is5xxServerError();
        }
        return false;
    }

    private static String ensureSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) return sessionId;
        return "s_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String normalize(String base) {
        if (base == null) return "";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "";
        return path.startsWith("/") ? path : "/" + path;
    }
}
