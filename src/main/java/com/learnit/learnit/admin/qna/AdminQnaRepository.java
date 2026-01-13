package com.learnit.learnit.admin.qna;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AdminQnaRepository {

    List<AdminQnaDTO> selectQnas(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("type") String type,
            @Param("status") String status,
            @Param("searchField") String searchField,
            @Param("search") String search,
            @Param("searchQnaId") Integer searchQnaId,
            @Param("instructorUserId") Long instructorUserId
    );

    int countQnas(
            @Param("type") String type,
            @Param("status") String status,
            @Param("searchField") String searchField,
            @Param("search") String search,
            @Param("searchQnaId") Integer searchQnaId,
            @Param("instructorUserId") Long instructorUserId
    );

    AdminQnaDTO selectQnaDetail(@Param("qnaId") int qnaId);

    Integer selectLatestStaffAnswerId(@Param("qnaId") int qnaId);

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

    Long selectInstructorUserIdByCourseId(@Param("courseId") int courseId);

    // ✅✅✅ 추가: 강의 첫 챕터 id 조회 (관리자 -> 강의 재생 화면으로 이동할 때 필요)
    Integer selectFirstChapterIdByCourseId(@Param("courseId") int courseId);
}
