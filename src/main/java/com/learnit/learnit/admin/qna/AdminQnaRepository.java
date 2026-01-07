package com.learnit.learnit.admin.qna;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminQnaRepository {

    List<AdminQnaDto> selectQnas(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("type") String type,
            @Param("status") String status,
            @Param("searchField") String searchField,
            @Param("search") String search,
            @Param("searchQnaId") Integer searchQnaId,
            @Param("instructorUserId") Long instructorUserId   // ✅ 추가
    );

    int countQnas(
            @Param("type") String type,
            @Param("status") String status,
            @Param("searchField") String searchField,
            @Param("search") String search,
            @Param("searchQnaId") Integer searchQnaId,
            @Param("instructorUserId") Long instructorUserId   // ✅ 추가
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

    // ✅ 추가: 강의 소유자(강사) 확인
    Long selectInstructorUserIdByCourseId(@Param("courseId") int courseId);
}
