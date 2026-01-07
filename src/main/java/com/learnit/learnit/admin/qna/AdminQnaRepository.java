package com.learnit.learnit.admin.qna;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminQnaRepository {

    List<AdminQnaDto> selectQnas(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("type") String type,        // LECTURE / SITE / null
            @Param("status") String status,    // ACTIVE / PASS / null
            @Param("searchField") String searchField, // QNA_ID / TITLE / WRITER
            @Param("search") String search,
            @Param("searchQnaId") Integer searchQnaId
    );

    int countQnas(
            @Param("type") String type,
            @Param("status") String status,
            @Param("searchField") String searchField,
            @Param("search") String search,
            @Param("searchQnaId") Integer searchQnaId
    );

    AdminQnaDto selectQnaDetail(@Param("qnaId") int qnaId);

    Integer selectLatestAnswerId(@Param("qnaId") int qnaId);

    void insertAnswer(
            @Param("qnaId") int qnaId,
            @Param("userId") long userId,
            @Param("content") String content
    );

    void updateAnswer(
            @Param("answerId") int answerId,
            @Param("content") String content
    );

    void updateResolved(
            @Param("qnaId") int qnaId,
            @Param("isResolved") String isResolved
    );

    void softDeleteAnswersByQnaId(@Param("qnaId") int qnaId);

    void softDeleteQuestionById(@Param("qnaId") int qnaId);
}
