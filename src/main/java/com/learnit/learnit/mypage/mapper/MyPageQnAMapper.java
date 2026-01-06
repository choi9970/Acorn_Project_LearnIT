package com.learnit.learnit.mypage.mapper;

import com.learnit.learnit.mypage.dto.QnADTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyPageQnAMapper {

    /**
     * 사용자가 작성한 Q&A 목록 조회
     */
    List<QnADTO> selectMyQnAList(@Param("userId") Long userId);
}

