package com.learnit.learnit.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminHomeController {
    private final AdminHomeService adminService;

    @GetMapping("/admin/home")
    public String adminHome(Model model) {
        try {
            AdminHome dashboardData = adminService.getDashboardData();
            if (dashboardData == null) {
                dashboardData = new AdminHome();
            }
            model.addAttribute("dashboard", dashboardData);
        } catch (Exception e) {
            log.error("관리자 홈 대시보드 데이터 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("dashboard", new AdminHome());
        }
        return "admin/admin-home";
    }
}
