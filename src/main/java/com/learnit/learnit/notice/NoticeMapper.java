package com.learnit.learnit.notice;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {

    List<Notice> findPage(@Param("limit") int limit,
                          @Param("offset") int offset);

    int countAll();

    Notice findById(@Param("id") Long id);
}
