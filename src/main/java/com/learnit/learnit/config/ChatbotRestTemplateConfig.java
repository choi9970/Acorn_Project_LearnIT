package com.learnit.learnit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatbotRestTemplateConfig {

    @Value("${chatbot.agent.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${chatbot.agent.read-timeout-ms:60000}")
    private int readTimeoutMs;

    @Bean(name = "chatbotRestTemplate")
    public RestTemplate chatbotRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
