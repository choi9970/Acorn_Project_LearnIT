package com.learnit.learnit.chatbot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QnaMapper {

    void insertQna(@Param("courseId") Integer courseId,
                   @Param("userId") Long userId,
                   @Param("content") String content);
}