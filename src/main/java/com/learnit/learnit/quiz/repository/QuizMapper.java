package com.learnit.learnit.quiz.repository;

import com.learnit.learnit.quiz.dto.Quiz;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizMapper {
    List<Quiz> selectQuizListByCourseId(Long courseId);

    Quiz selectQuizByQuizId(Long quizId);

    int countUserQuizHistory(@Param("userId") Long userId, @Param("quizId") Long quizId);

    void insertUserAnswer(@Param("userId") Long userId, 
                          @Param("questionId") Long questionId, 
                          @Param("optionId") Long optionId, 
                          @Param("isCorrect") String isCorrect);
}
