package com.learnit.learnit.mypage;

import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MypageController {

    private final DashboardService dashboardService;
    private final UserMapper userMapper;

    @GetMapping("/mypage")
    public String myPage() {
        // 마이페이지 기본 라우팅: /mypage → 대시보드 호출
        return "redirect:/mypage/dashboard";
    }

    @GetMapping("/mypage/dashboard")
    public String dashboard(Model model, HttpSession session) {
        // 세션에서 사용자 ID 가져오기
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
            return "redirect:/login";
        }
        
        DashboardDTO dashboard = dashboardService.getDashboardData(userId);
        model.addAttribute("dashboard", dashboard);
        
        // 사용자 정보 조회 및 추가
        UserDTO user = userMapper.selectUserById(userId);
        model.addAttribute("user", user);
        
        return "mypage/dashboard/dashboard";
    }
}

