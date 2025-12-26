package com.learnit.learnit.quiz.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuizSubmit {
    private Long quizId;               // 어떤 퀴즈인지
    private List<UserAnswer> answers; // 사용자의 답안 리스트
}