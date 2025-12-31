package com.learnit.learnit.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {
    private Long quizId;
    private List<UserAnswerRequest> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAnswerRequest {
        private Long questionId;
        private Long optionId;
    }
}
