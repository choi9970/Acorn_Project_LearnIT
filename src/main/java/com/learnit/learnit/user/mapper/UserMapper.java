package com.learnit.learnit.user.mapper;

import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    String now(); // mapper XML의 id="now" 메서드

    // DTO 조회 (기존)
    UserDTO selectUserById(@Param("userId") Long userId);

    // Entity 조회
    User selectUserEntityById(@Param("userId") Long userId);
    User selectUserByEmail(@Param("email") String email);
    User selectUserByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId);

    // 사용자 생성 및 수정
    void insertUser(User user);
    void updateUser(User user);

    // 교수자 검색
    java.util.List<UserDTO> searchInstructors(@Param("keyword") String keyword);
}