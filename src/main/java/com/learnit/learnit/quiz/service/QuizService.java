package com.learnit.learnit.quiz.service;

import com.learnit.learnit.quiz.dto.QuizOption;
import com.learnit.learnit.quiz.dto.UserAnswer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuizService {


    public int submitQuiz(Long userId, Long quizId, List<UserAnswer> answers){
        int correctCount = 0;
        int totalCount = answers.size();

        for (UserAnswer ans : answers){
            QuizOption option = quizOptionRepository.
        }
    }
}
