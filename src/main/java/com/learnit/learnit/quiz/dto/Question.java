package com.learnit.learnit.quiz.dto;
import lombok.Data;
import java.util.List;

@Data
public class Question {
    private Long questionId;
    private String content;
    private String explanation;
    private List<QuizOption> options;
}