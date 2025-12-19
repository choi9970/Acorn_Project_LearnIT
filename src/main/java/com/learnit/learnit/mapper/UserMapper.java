package com.learnit.learnit.mapper;

import com.learnit.learnit.mypage.dto.UserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    String now(); // mapper XML의 id="now" 메서드
    
    UserDTO selectUserById(@Param("userId") Long userId);
}
