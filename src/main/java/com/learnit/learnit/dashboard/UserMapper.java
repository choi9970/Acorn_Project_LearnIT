package com.learnit.learnit.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    String now(); // mapper XML의 id="now" 메서드
    
    UserDTO selectUserById(@Param("userId") Long userId);
}

