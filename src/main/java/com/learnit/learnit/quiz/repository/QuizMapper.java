package com.learnit.learnit.quiz.repository;

import com.learnit.learnit.quiz.dto.Quiz;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QuizMapper {
    Quiz selectQuizByChapterId(Long chapterId);
}
