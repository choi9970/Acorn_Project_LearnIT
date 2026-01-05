package com.learnit.learnit.admin.userrole.dto;

import lombok.Data;

/**
 * ✅ 필요한 이유:
 * - status + (nickname/phone optional) 구조를 안정적으로 요청 바인딩하기 위해
 */
@Data
public class UpdateUserStatusDTO {
    private String status;   // SIGNUP_PENDING/ACTIVE/BANNED/DELETE
    private String nickname; // SIGNUP_PENDING(소셜)->ACTIVE일 때만 필요
    private String phone;    // SIGNUP_PENDING(소셜)->ACTIVE일 때만 필요
}
