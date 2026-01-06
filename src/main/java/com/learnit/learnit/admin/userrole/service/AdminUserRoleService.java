package com.learnit.learnit.admin.userrole.service;

import com.learnit.learnit.admin.userrole.dto.UpdateUserRoleDTO;
import com.learnit.learnit.admin.userrole.dto.UpdateUserStatusDTO;
import com.learnit.learnit.admin.userrole.mapper.AdminUserRoleMapper;
import com.learnit.learnit.user.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class AdminUserRoleService {

    private final AdminUserRoleMapper mapper;

    private void requireGlobalAdmin() {
        Long loginUserId = SessionUtils.requireLoginUserId();
        if (mapper.isGlobalAdmin(loginUserId) <= 0) {
            throw new ResponseStatusException(FORBIDDEN, "전체 관리자만 접근 가능합니다.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchUsers(String type, String keyword, int page, int size) {
        requireGlobalAdmin();

        int safePage = Math.max(page, 1);
        int safeSize = (size <= 0) ? 7 : Math.min(size, 50);
        int offset = (safePage - 1) * safeSize;

        if (!List.of("email", "name", "userId").contains(type)) type = "email";
        if (keyword == null) keyword = "";

        if ("userId".equals(type) && !isBlank(keyword)) {
            try { Long.parseLong(keyword.trim()); }
            catch (Exception e) {
                return Map.of(
                        "items", Collections.emptyList(),
                        "page", safePage,
                        "size", safeSize,
                        "totalPages", 0,
                        "totalCount", 0
                );
            }
        }

        int total = mapper.countUsers(type, keyword);
        int totalPages = (int) Math.ceil(total / (double) safeSize);

        List<Map<String, Object>> items = mapper.searchUsers(type, keyword, offset, safeSize);

        for (Map<String, Object> u : items) {
            String role = String.valueOf(u.get("role"));
            if ("SUB_ADMIN".equals(role)) {
                Long userId = ((Number) u.get("userId")).longValue();
                List<Map<String, Object>> managed = mapper.findManagedCourses(userId);
                u.put("managedCourses", managed);
            } else {
                u.put("managedCourses", Collections.emptyList());
            }
        }

        return Map.of(
                "items", items,
                "page", safePage,
                "size", safeSize,
                "totalPages", totalPages,
                "totalCount", total
        );
    }

    @Transactional
    public void updateRole(Long targetUserId, UpdateUserRoleDTO dto) {
        requireGlobalAdmin();

        if (dto == null || isBlank(dto.getRole())) {
            throw new ResponseStatusException(BAD_REQUEST, "role이 필요합니다.");
        }

        Map<String, Object> user = mapper.findUserPolicy(targetUserId);
        if (user == null) throw new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다.");

        String provider = (String) user.get("provider");
        boolean isSocial = provider != null && !"local".equalsIgnoreCase(provider);

        String newRole = dto.getRole().trim();
        if (!List.of("USER", "SUB_ADMIN", "ADMIN").contains(newRole)) {
            throw new ResponseStatusException(BAD_REQUEST, "role 값이 올바르지 않습니다.");
        }

        if (isSocial && ("ADMIN".equals(newRole) || "SUB_ADMIN".equals(newRole))) {
            throw new ResponseStatusException(BAD_REQUEST, "소셜 가입 회원에게 ADMIN/SUB_ADMIN 권한을 부여할 수 없습니다.");
        }

        mapper.updateUserRole(targetUserId, newRole);

        mapper.deleteAdminUserRoles(targetUserId);

        if ("USER".equals(newRole)) return;

        Integer roleId = mapper.findAdminRoleIdByCode(newRole);
        if (roleId == null) throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "admin_role(code=" + newRole + ")이 없습니다.");

        if ("ADMIN".equals(newRole)) {
            mapper.insertAdminUserRole(targetUserId, roleId, null);
            return;
        }

        List<Integer> courseIds = dto.getCourseIds();
        if (courseIds == null || courseIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "SUB_ADMIN은 1개 이상의 관리 강의가 필요합니다.");
        }

        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer cid : courseIds) {
            if (cid != null && cid > 0) unique.add(cid);
        }
        if (unique.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "유효한 courseIds가 없습니다.");

        for (Integer cid : unique) {
            mapper.insertAdminUserRole(targetUserId, roleId, cid);
        }
    }

    @Transactional
    public void updateStatus(Long targetUserId, UpdateUserStatusDTO dto) {
        requireGlobalAdmin();

        if (dto == null || isBlank(dto.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "status가 필요합니다.");
        }

        Map<String, Object> user = mapper.findUserPolicy(targetUserId);
        if (user == null) throw new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다.");

        String curr = String.valueOf(user.get("status"));
        String provider = (String) user.get("provider");
        boolean isSocial = provider != null && !"local".equalsIgnoreCase(provider);

        String next = dto.getStatus().trim();
        if (!List.of("SIGNUP_PENDING", "ACTIVE", "BANNED", "DELETE").contains(next)) {
            throw new ResponseStatusException(BAD_REQUEST, "status 값이 올바르지 않습니다.");
        }

        if ("SIGNUP_PENDING".equals(curr)) {
            if (!"ACTIVE".equals(next)) {
                throw new ResponseStatusException(BAD_REQUEST, "SIGNUP_PENDING은 ACTIVE로만 변경 가능합니다.");
            }
            if (isSocial) {
                if (isBlank(dto.getNickname()) || isBlank(dto.getPhone())) {
                    throw new ResponseStatusException(BAD_REQUEST, "SIGNUP_PENDING→ACTIVE는 nickname/phone이 필요합니다.");
                }
                mapper.forceActivateSocialPending(targetUserId, dto.getNickname().trim(), dto.getPhone().trim());
                return;
            }
            mapper.updateUserStatus(targetUserId, "ACTIVE");
            return;
        }

        if ("ACTIVE".equals(curr)) {
            if (!List.of("ACTIVE", "BANNED", "DELETE").contains(next)) {
                throw new ResponseStatusException(BAD_REQUEST, "ACTIVE는 BANNED/DELETE로만 변경 가능합니다.");
            }
            mapper.updateUserStatus(targetUserId, next);
            return;
        }

        if ("BANNED".equals(curr) || "DELETE".equals(curr)) {
            if (!"ACTIVE".equals(next)) {
                throw new ResponseStatusException(BAD_REQUEST, curr + "는 ACTIVE로만 변경 가능합니다.");
            }
            mapper.updateUserStatus(targetUserId, "ACTIVE");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchCourses(String keyword, int page, int size) {
        requireGlobalAdmin();

        int safePage = Math.max(page, 1);
        int safeSize = (size <= 0) ? 7 : Math.min(size, 50);
        int offset = (safePage - 1) * safeSize;

        if (keyword == null) keyword = "";

        int total = mapper.countCourses(keyword);
        int totalPages = (int) Math.ceil(total / (double) safeSize);

        List<Map<String, Object>> items = mapper.searchCourses(keyword, offset, safeSize);

        return Map.of(
                "items", items,
                "page", safePage,
                "size", safeSize,
                "totalPages", totalPages,
                "totalCount", total
        );
    }

    // ✅ SUB_ADMIN 태그 “삭제(×)” - 즉시 반영
    @Transactional
    public void removeSubAdminCourse(Long targetUserId, Integer courseId) {
        requireGlobalAdmin();

        if (targetUserId == null || courseId == null || courseId <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "userId/courseId가 올바르지 않습니다.");
        }

        Map<String, Object> user = mapper.findUserPolicy(targetUserId);
        if (user == null) throw new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다.");

        String role = String.valueOf(user.get("role"));
        if (!"SUB_ADMIN".equals(role)) {
            throw new ResponseStatusException(BAD_REQUEST, "SUB_ADMIN 사용자만 관리 강의를 삭제할 수 있습니다.");
        }

        Integer subAdminRoleId = mapper.findAdminRoleIdByCode("SUB_ADMIN");
        if (subAdminRoleId == null) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "admin_role(code=SUB_ADMIN)이 없습니다.");
        }

        int remain = mapper.countSubAdminCourses(targetUserId, subAdminRoleId);
        if (remain <= 1) {
            throw new ResponseStatusException(BAD_REQUEST, "SUB_ADMIN은 최소 1개 이상의 관리 강의가 필요합니다.");
        }

        int deleted = mapper.deleteSubAdminCourse(targetUserId, subAdminRoleId, courseId);
        if (deleted <= 0) {
            throw new ResponseStatusException(NOT_FOUND, "삭제할 관리 강의를 찾을 수 없습니다.");
        }
    }
}
