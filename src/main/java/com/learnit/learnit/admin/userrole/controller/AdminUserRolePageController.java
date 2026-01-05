package com.learnit.learnit.admin.userrole.controller;

import com.learnit.learnit.admin.userrole.mapper.AdminUserRoleMapper;
import com.learnit.learnit.user.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminUserRolePageController {

    private final AdminUserRoleMapper mapper;

    @GetMapping("/admin/role")
    public String page(Model model) {
        Long loginUserId = SessionUtils.requireLoginUserId();

        // ✅ 전체 관리자: admin_user_role + admin_role(code='ADMIN') 존재
        if (mapper.isGlobalAdmin(loginUserId) <= 0) {
            return "redirect:/login";
        }

        model.addAttribute("activeTab", "role");
        return "admin/userrole/fragments/adminUserRoleList";
    }
}
