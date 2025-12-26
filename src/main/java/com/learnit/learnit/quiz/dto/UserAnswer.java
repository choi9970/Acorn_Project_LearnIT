package com.learnit.learnit.quiz.dto;

import lombok.Data;

@Data
public class UserAnswer {
    private Long questionId;
    private Long optionId;
}
