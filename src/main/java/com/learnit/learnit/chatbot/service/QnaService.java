package com.learnit.learnit.chatbot.service;

import com.learnit.learnit.chatbot.mapper.QnaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상담원 문의(QnA) 저장 서비스
 */
@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaMapper qnaMapper;

    @Transactional
    public void createQna(Integer courseId, Long userId, String content) {
        qnaMapper.insertQna(courseId, userId, content);
    }
}