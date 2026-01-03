package com.learnit.learnit.quiz.controller;

import com.learnit.learnit.quiz.dto.Quiz;
import com.learnit.learnit.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.learnit.learnit.quiz.dto.QuizSubmitRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public ResponseEntity<?> getQuiz(@RequestParam("chapterId") Long quizId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        Quiz quiz = quizService.getQuiz(quizId, userId);

        if (quiz == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(quiz);
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody QuizSubmitRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return ResponseEntity.status(403).body("로그인이 필요합니다.");
        }

        quizService.submitQuiz(userId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("isPassed", true); // 추후 채점 로직 추가 가능
        
        return ResponseEntity.ok(result);
    }
}