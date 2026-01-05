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

    // 유저 검색 + 페이징(7건)  --- 응답은 Map
    @GetMapping("/users")
    public Map<String, Object> users(
            @RequestParam(defaultValue = "email") String type,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "7") int size
    ) {
        return service.searchUsers(type, keyword, page, size);
    }

    // 권한 변경 --- 요청은 DTO(필수)
    @PostMapping("/users/{userId}/role")
    public Map<String, Object> updateRole(@PathVariable Long userId, @RequestBody UpdateUserRoleDTO dto) {
        service.updateRole(userId, dto);
        return Map.of("ok", true);
    }

    // 상태 변경 --- 요청은 DTO(필수)
    @PostMapping("/users/{userId}/status")
    public Map<String, Object> updateStatus(@PathVariable Long userId, @RequestBody UpdateUserStatusDTO dto) {
        service.updateStatus(userId, dto);
        return Map.of("ok", true);
    }

    // 강의 검색(ajax) --- 응답은 Map
    @GetMapping("/courses")
    public Map<String, Object> courses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "7") int size
    ) {
        return service.searchCourses(keyword, page, size);
    }
}
