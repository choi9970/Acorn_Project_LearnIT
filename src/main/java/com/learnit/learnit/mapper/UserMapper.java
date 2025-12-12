package com.learnit.learnit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    String now(); // mapper XML의 id="now" 메서드
}
