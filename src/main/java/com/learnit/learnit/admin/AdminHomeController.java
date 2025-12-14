package com.learnit.learnit.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {
    @GetMapping("/admin/home")
    public String adminHome() {
        return "admin/admin-main";
    }
}
