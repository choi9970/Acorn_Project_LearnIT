package com.learnit.learnit.mypage.mapper;

import com.learnit.learnit.mypage.dto.MyQnADTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyQnAMapper {

    /**
     * 사용자가 작성한 Q&A 목록 조회 (페이징)
     */
    List<MyQnADTO> selectMyQnAList(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 사용자가 작성한 Q&A 총 개수 조회
     */
    int countMyQnAList(@Param("userId") Long userId);

    /**
     * 관리자(ADMIN)가 볼 수 있는 모든 Q&A 목록 조회 (페이징)
     */
    List<MyQnADTO> selectAdminQnAList(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 관리자(ADMIN)가 볼 수 있는 모든 Q&A 총 개수 조회
     */
    int countAdminQnAList();

    /**
     * 서브 어드민(SUB_ADMIN)이 관리하는 강의의 Q&A 목록 조회 (페이징)
     */
    List<MyQnADTO> selectSubAdminQnAList(@Param("managedCourseIds") List<Integer> managedCourseIds, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 서브 어드민(SUB_ADMIN)이 관리하는 강의의 Q&A 총 개수 조회
     */
    int countSubAdminQnAList(@Param("managedCourseIds") List<Integer> managedCourseIds);

    /**
     * 서브 어드민이 관리하는 강의 ID 목록 조회
     */
    List<Integer> selectManagedCourseIds(@Param("userId") Long userId);
}

