package com.learnit.learnit.mypage.dashboard.controller;

import com.learnit.learnit.mapper.UserMapper;
import com.learnit.learnit.mypage.dashboard.dto.DashboardDTO;
import com.learnit.learnit.mypage.dashboard.service.DashboardService;
import com.learnit.learnit.mypage.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserMapper userMapper;

    @GetMapping("/mypage/dashboard")
    public String dashboard(Model model) {
        // TODO: 실제 사용자 ID를 세션에서 가져오도록 수정 필요
        Long userId = 1L; // 임시로 1L 사용
        
        DashboardDTO dashboard = dashboardService.getDashboardData(userId);
        model.addAttribute("dashboard", dashboard);
        
        // 사용자 정보 조회 및 추가
        UserDTO user = userMapper.selectUserById(userId);
        model.addAttribute("user", user);
        
        return "mypage/dashboard/dashboard";
    }
}

