package com.learnit.learnit.admin.userrole.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminUserRoleMapper {

    // 전체 관리자 여부(aur + ar.code='ADMIN')
    int isGlobalAdmin(@Param("userId") Long userId);

    // admin_role_id (하드코딩 금지)
    Integer findAdminRoleIdByCode(@Param("code") String code);

    // 검색/페이징
    int countUsers(@Param("type") String type, @Param("keyword") String keyword);

    List<Map<String, Object>> searchUsers(@Param("type") String type,
                                          @Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    // 정책 체크용(user status/provider)
    Map<String, Object> findUserPolicy(@Param("userId") Long userId);

    // SUB_ADMIN 관리 강의
    List<Map<String, Object>> findManagedCourses(@Param("userId") Long userId);

    // users 업데이트
    int updateUserRole(@Param("userId") Long userId, @Param("role") String role);

    int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    int forceActivateSocialPending(@Param("userId") Long userId,
                                   @Param("nickname") String nickname,
                                   @Param("phone") String phone);

    // admin_user_role 유지
    int deleteAdminUserRoles(@Param("userId") Long userId);

    int insertAdminUserRole(@Param("userId") Long userId,
                            @Param("adminRoleId") int adminRoleId,
                            @Param("courseId") Integer courseId);

    // 강의 검색
    int countCourses(@Param("keyword") String keyword);

    List<Map<String, Object>> searchCourses(@Param("keyword") String keyword,
                                            @Param("offset") int offset,
                                            @Param("size") int size);
}
