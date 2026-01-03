package com.learnit.learnit.quiz.dto;
import lombok.Data;

@Data
public class QuizOption {
    private Long optionId;
    private String content;
    private String isCorrect;
}