package com.learnit.learnit.quiz.dto;
import lombok.Data;
import java.util.List;

@Data
public class Quiz {
    private Long quizId;
    private List<Question> questions;
}