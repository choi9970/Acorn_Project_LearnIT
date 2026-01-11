package com.learnit.learnit.quiz.repository;

import com.learnit.learnit.quiz.dto.Quiz;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizMapper {
    List<Quiz> selectQuizListByCourseId(Long courseId);
    List<Quiz> selectFullQuizzesByCourseId(Long courseId);

    Quiz selectQuizByQuizId(Long quizId);

    /**
     * 퀴즈 ID로 courseId 조회
     */
    Long selectCourseIdByQuizId(@Param("quizId") Long quizId);

    int countUserQuizHistory(@Param("userId") Long userId, @Param("quizId") Long quizId);

    /**
     * 퀴즈 통과 여부 확인 (정답률 60% 이상)
     */
    boolean isQuizPassed(@Param("userId") Long userId, @Param("quizId") Long quizId);

    void insertUserAnswer(@Param("userId") Long userId, 
                          @Param("questionId") Long questionId, 
                          @Param("optionId") Long optionId, 
                          @Param("isCorrect") String isCorrect);

    // Admin Methods
    void insertQuiz(Quiz quiz); // Requires quiz.courseId to be set (not in DTO, need to extend or use map)
    // Actually Quiz DTO doesn't have courseId.
    // Let's use @Param for courseId if needed or update Quiz DTO.
    // But Quiz DTO is in `quiz.dto` package. I shouldn't modify it if it breaks other things.
    // Use @Param for insertQuiz is better.
    void insertQuiz(@Param("courseId") Long courseId, @Param("quiz") Quiz quiz);

    void updateQuiz(Quiz quiz);
    void deleteQuiz(Long quizId);

    void insertQuestion(@Param("quizId") Long quizId, @Param("question") com.learnit.learnit.quiz.dto.Question question);
    void deleteQuestionsByQuizId(Long quizId);

    void insertOption(@Param("questionId") Long questionId, @Param("option") com.learnit.learnit.quiz.dto.QuizOption option);
    void deleteOptionsByQuestionId(Long questionId);
}
