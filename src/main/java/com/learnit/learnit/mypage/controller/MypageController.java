package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.DashboardDTO;
import com.learnit.learnit.mypage.dto.GitHubAnalysisDTO;
import com.learnit.learnit.mypage.dto.ProfileDTO;
import com.learnit.learnit.mypage.dto.SkillChartDTO;
import com.learnit.learnit.mypage.service.DashboardService;
import com.learnit.learnit.mypage.service.GitHubAnalysisService;
import com.learnit.learnit.mypage.service.ProfileService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MypageController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final ProfileService profileService;
    private final GitHubAnalysisService githubAnalysisService;

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
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);
        
        return "mypage/dashboard/dashboard";
    }

    /**
     * 프로필 페이지 조회
     */
    @GetMapping("/mypage/profile")
    public String profile(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);

        // 프로필 정보 조회
        ProfileDTO profile = profileService.getProfile(userId);
        model.addAttribute("profile", profile);
        
        // 저장된 GitHub 분석 결과 조회
        GitHubAnalysisDTO savedAnalysis = githubAnalysisService.getSavedGitHubAnalysis(userId);
        
        if (savedAnalysis != null) {
            SkillChartDTO skillChart = githubAnalysisService.generateSkillChart(savedAnalysis);
            model.addAttribute("githubAnalysis", savedAnalysis);
            model.addAttribute("skillChart", skillChart);
        }

        return "mypage/profile/profile";
    }
}

