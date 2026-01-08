package com.learnit.learnit.admin.userrole;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminUserRoleMapper {

    int isGlobalAdmin(@Param("userId") Long userId);

    Integer findAdminRoleIdByCode(@Param("code") String code);

    int countUsers(@Param("type") String type,
                   @Param("keyword") String keyword,
                   @Param("statuses") List<String> statuses,
                   @Param("roles") List<String> roles);

    List<Map<String, Object>> searchUsers(@Param("type") String type,
                                          @Param("keyword") String keyword,
                                          @Param("statuses") List<String> statuses,
                                          @Param("roles") List<String> roles,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    // ✅ 필터 목록(전체 결과 기준) - GROUP BY
    List<Map<String, Object>> groupUsersByStatus(@Param("type") String type,
                                                 @Param("keyword") String keyword,
                                                 @Param("roles") List<String> roles);

    List<Map<String, Object>> groupUsersByRole(@Param("type") String type,
                                               @Param("keyword") String keyword,
                                               @Param("statuses") List<String> statuses);

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

    // ✅ 강의 검색(페이징 없음)
    List<Map<String, Object>> searchCourses(@Param("keyword") String keyword);

    // ✅ SUB_ADMIN 관리강의 삭제/카운트
    int countSubAdminCourses(@Param("userId") Long userId,
                             @Param("adminRoleId") int adminRoleId);

    int deleteSubAdminCourse(@Param("userId") Long userId,
                             @Param("adminRoleId") int adminRoleId,
                             @Param("courseId") int courseId);
}
