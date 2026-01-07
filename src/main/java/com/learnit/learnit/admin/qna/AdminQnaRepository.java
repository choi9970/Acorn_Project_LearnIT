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
            @Param("search") String search
    );

    int countQnas(
            @Param("type") String type,
            @Param("status") String status,
            @Param("search") String search
    );

    AdminQnaDto selectQnaDetail(@Param("qnaId") int qnaId);

    Integer selectLatestAnswerId(@Param("qnaId") int qnaId);

    void insertAnswer(@Param("qnaId") int qnaId,
                      @Param("userId") int userId,
                      @Param("content") String content);

    void updateAnswer(@Param("answerId") int answerId,
                      @Param("content") String content);

    void updateResolved(@Param("qnaId") int qnaId,
                        @Param("isResolved") String isResolved);

    void deleteAnswersByQnaId(@Param("qnaId") int qnaId);

    void deleteQuestionById(@Param("qnaId") int qnaId);

    // ✅✅ 추가: 다음 qna_id = MAX(qna_id)+1 (비어있으면 1)
    int selectNextQnaId();

    // ✅✅ 추가: AUTO_INCREMENT 재설정 (MAX+1로)
    void resetQnaAutoIncrement(@Param("nextId") int nextId);
}
