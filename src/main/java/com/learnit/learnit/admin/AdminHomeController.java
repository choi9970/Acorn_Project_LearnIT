package com.learnit.learnit.admin;

import com.learnit.learnit.admin.AdminHome;
import com.learnit.learnit.admin.AdminHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminHomeController {
    private final AdminHomeService adminService;

    @GetMapping("/admin/home")
    public String adminHome(Model model) {

        AdminHome dashboardData = adminService.getDashboardData();

        model.addAttribute("dashboard", dashboardData);

        return "admin/admin-home";
    }
}
