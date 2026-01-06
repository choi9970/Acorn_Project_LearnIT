package com.learnit.learnit.admin.userrole.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminUserRoleMapper {

    int isGlobalAdmin(@Param("userId") Long userId);

    Integer findAdminRoleIdByCode(@Param("code") String code);

    int countUsers(@Param("type") String type, @Param("keyword") String keyword);

    List<Map<String, Object>> searchUsers(@Param("type") String type,
                                          @Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    Map<String, Object> findUserPolicy(@Param("userId") Long userId);

    List<Map<String, Object>> findManagedCourses(@Param("userId") Long userId);

    int updateUserRole(@Param("userId") Long userId, @Param("role") String role);

    int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    int forceActivateSocialPending(@Param("userId") Long userId,
                                   @Param("nickname") String nickname,
                                   @Param("phone") String phone);

    int deleteAdminUserRoles(@Param("userId") Long userId);

    int insertAdminUserRole(@Param("userId") Long userId,
                            @Param("adminRoleId") int adminRoleId,
                            @Param("courseId") Integer courseId);

    int countCourses(@Param("keyword") String keyword);

    List<Map<String, Object>> searchCourses(@Param("keyword") String keyword,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    // ✅ 추가: SUB_ADMIN 관리강의 삭제/카운트
    int countSubAdminCourses(@Param("userId") Long userId,
                             @Param("adminRoleId") int adminRoleId);

    int deleteSubAdminCourse(@Param("userId") Long userId,
                             @Param("adminRoleId") int adminRoleId,
                             @Param("courseId") int courseId);
}
