package com.learnit.learnit.admin.userrole;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminUserRoleApiController {

    private final AdminUserRoleService service;

    @GetMapping("/users")
    public Map<String, Object> users(
            @RequestParam(defaultValue = "email") String type,
            @RequestParam(defaultValue = "") String keyword,
            // ✅ 필터(다중 선택): 콤마로 전달 (ex: ACTIVE,BANNED)
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) String roles,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "7") int size
    ) {
        return service.searchUsers(type, keyword, statuses, roles, page, size);
    }

    @PostMapping("/users/{userId}/role")
    public Map<String, Object> updateRole(@PathVariable Long userId, @RequestBody UpdateUserRoleDTO dto) {
        service.updateRole(userId, dto);
        return Map.of("ok", true);
    }

    @PostMapping("/users/{userId}/status")
    public Map<String, Object> updateStatus(@PathVariable Long userId, @RequestBody UpdateUserStatusDTO dto) {
        service.updateStatus(userId, dto);
        return Map.of("ok", true);
    }

    /**
     * ✅ 강의 검색
     * - keyword 빈칸이면 전체 강의
     * - keyword 있으면 course_id / title 부분일치(대소문자 무시)
     */
    @GetMapping("/courses")
    public List<Map<String, Object>> courses(
            @RequestParam(defaultValue = "") String keyword
    ) {
        return service.searchCourses(keyword);
    }

    // ✅ SUB_ADMIN 태그 “삭제(×)” - 즉시 서버 반영
    @DeleteMapping("/users/{userId}/sub-admin/courses/{courseId}")
    public Map<String, Object> deleteSubAdminCourse(@PathVariable Long userId, @PathVariable Integer courseId) {
        service.removeSubAdminCourse(userId, courseId);
        return Map.of("ok", true);
    }
}
