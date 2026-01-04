package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.QnADTO;
import com.learnit.learnit.mypage.mapper.MyPageQnAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageQnAService {

    private final MyPageQnAMapper qnAMapper;

    /**
     * 사용자가 작성한 Q&A 목록 조회
     */
    public List<QnADTO> getMyQnAList(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }
        return qnAMapper.selectMyQnAList(userId);
    }
}

