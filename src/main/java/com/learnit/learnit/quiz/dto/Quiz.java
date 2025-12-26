package com.learnit.learnit.quiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class Quiz {
    private Long quizId;
    private String title;
    private List<Question> questions;
}
