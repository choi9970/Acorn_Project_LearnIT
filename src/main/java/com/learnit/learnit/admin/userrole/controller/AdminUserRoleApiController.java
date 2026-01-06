package com.learnit.learnit.admin.userrole.controller;

import com.learnit.learnit.admin.userrole.dto.UpdateUserRoleDTO;
import com.learnit.learnit.admin.userrole.dto.UpdateUserStatusDTO;
import com.learnit.learnit.admin.userrole.service.AdminUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "7") int size
    ) {
        return service.searchUsers(type, keyword, page, size);
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

    @GetMapping("/courses")
    public Map<String, Object> courses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "7") int size
    ) {
        return service.searchCourses(keyword, page, size);
    }

    // ✅ SUB_ADMIN 태그 “삭제(×)” - 즉시 서버 반영
    @DeleteMapping("/users/{userId}/sub-admin/courses/{courseId}")
    public Map<String, Object> deleteSubAdminCourse(@PathVariable Long userId, @PathVariable Integer courseId) {
        service.removeSubAdminCourse(userId, courseId);
        return Map.of("ok", true);
    }
}
