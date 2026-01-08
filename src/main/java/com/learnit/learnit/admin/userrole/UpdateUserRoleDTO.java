package com.learnit.learnit.admin.userrole;

import lombok.Data;

import java.util.List;

/**
 * ✅ 필요한 이유:
 * - @RequestBody로 role + courseIds를 안전하게 바인딩/검증하기 위해
 * - Map으로 받으면 타입 캐스팅/검증이 매번 필요해 런타임 오류가 잦음
 */
@Data
public class UpdateUserRoleDTO {
    private String role;             // USER/SUB_ADMIN/ADMIN
    private List<Integer> courseIds; // SUB_ADMIN일 때만 사용
}
