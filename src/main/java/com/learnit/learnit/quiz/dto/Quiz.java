package com.learnit.learnit.quiz.dto;

import lombok.Data;
import java.util.List;

@Data
public class Quiz {
    private Long quizId;
    private String title;
    private String sectionTitle;
    private String type;
    private boolean isSubmitted;
    private List<Question> questions;
}