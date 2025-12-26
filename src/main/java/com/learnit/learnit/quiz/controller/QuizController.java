package com.learnit.learnit.quiz.controller;

import com.learnit.learnit.quiz.dto.Quiz;
import com.learnit.learnit.quiz.repository.QuizMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizMapper quizMapper;

    @GetMapping
    public ResponseEntity<?> getQuiz(@RequestParam Long chapterId) {
        Quiz quiz = quizMapper.selectQuizByChapterId(chapterId);
        if (quiz == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(quiz);
    }
}
