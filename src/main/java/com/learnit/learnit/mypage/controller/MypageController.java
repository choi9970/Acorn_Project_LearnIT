package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.MyCertificateDTO;
import com.learnit.learnit.mypage.dto.MyDashboardDTO;
import com.learnit.learnit.mypage.dto.MyDailyCourseDTO;
import com.learnit.learnit.mypage.dto.MyDailyGoalDTO;
import com.learnit.learnit.mypage.dto.MyGitHubAnalysisDTO;
import com.learnit.learnit.mypage.dto.MyProfileDTO;
import com.learnit.learnit.mypage.dto.MySkillChartDTO;
import com.learnit.learnit.mypage.dto.MyWeeklyLearningDTO;
import com.learnit.learnit.mypage.dto.MyCalendarSummaryDTO;
import com.learnit.learnit.courseVideo.service.CourseVideoService;
import com.learnit.learnit.mypage.service.MyDashboardService;
import com.learnit.learnit.mypage.service.MyGitHubAnalysisService;
import com.learnit.learnit.mypage.service.MyProfileService;
import com.learnit.learnit.user.util.SessionUtils;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MypageController {

    private final MyDashboardService dashboardService;
    private final UserService userService;
    private final MyProfileService profileService;
    private final MyGitHubAnalysisService githubAnalysisService;
    private final CourseVideoService courseVideoService;


    @GetMapping("/mypage")
    public String myPage() {
        // 마이페이지 기본 라우팅: /mypage → 대시보드 호출
        return "redirect:/mypage/dashboard";
    }

    @GetMapping("/mypage/dashboard")
    public String dashboard(Model model, HttpSession session) {
        // 세션에서 사용자 ID 가져오기
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
            return "redirect:/login";
        }
        
        MyDashboardDTO dashboard = dashboardService.getDashboardData(userId);
        model.addAttribute("dashboard", dashboard);
        
        // 사용자 정보 조회 및 추가
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);
        
        return "mypage/dashboard/myDashboard";
    }

    /**
     * 프로필 페이지 조회
     */
    @GetMapping("/mypage/profile")
    public String profile(Model model, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);

        // 프로필 정보 조회
        MyProfileDTO profile = profileService.getProfile(userId);
        model.addAttribute("profile", profile);

        // 수료증 목록 조회
        java.util.List<MyCertificateDTO> certificates = profileService.getCertificates(userId);
        model.addAttribute("certificates", certificates);

        // 저장된 GitHub 분석 결과 조회
        MyGitHubAnalysisDTO savedAnalysis = githubAnalysisService.getSavedGitHubAnalysis(userId);
        
        if (savedAnalysis != null) {
            MySkillChartDTO skillChart = githubAnalysisService.generateSkillChart(savedAnalysis);
            model.addAttribute("githubAnalysis", savedAnalysis);
            model.addAttribute("skillChart", skillChart);
        }

        return "mypage/profile/myProfile";
    }


    /**
     * 주간 학습 데이터 조회 (AJAX용)
     */
    @GetMapping(value = "/api/mypage/weekly-learning", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getWeeklyLearning(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String startDate,
            HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "로그인이 필요한 서비스입니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        try {
            LocalDate weekStart;
            if (startDate != null && !startDate.isEmpty()) {
                weekStart = LocalDate.parse(startDate);
            } else {
                LocalDate now = LocalDate.now();
                weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
            }

            MyWeeklyLearningDTO data = dashboardService.getWeeklyLearningDataByStartDate(userId, year, month, weekStart);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "주간 학습 데이터 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 캘린더 데이터 조회 (AJAX용)
     */
    @GetMapping(value = "/api/mypage/calendar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getCalendar(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "로그인이 필요한 서비스입니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        try {
            MyCalendarSummaryDTO data = dashboardService.getCalendarData(userId, year, month);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "캘린더 데이터 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 일일 학습 목표 저장 (AJAX용)
     */
    @PostMapping(value = "/api/mypage/daily-goals", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveDailyGoals(
            @RequestBody Map<String, Object> goals,
            HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "로그인이 필요한 서비스입니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        try {
            Integer classGoal = goals.get("classGoal") != null ? 
                Integer.parseInt(goals.get("classGoal").toString()) : null;
            Integer timeGoal = goals.get("timeGoal") != null ? 
                Integer.parseInt(goals.get("timeGoal").toString()) : null;
            Integer interpreterGoal = goals.get("noteGoal") != null ? 
                Integer.parseInt(goals.get("noteGoal").toString()) : null; // noteGoal은 interpreterGoal로 매핑
            
            MyDailyGoalDTO savedGoal = dashboardService.saveDailyGoal(userId, classGoal, timeGoal, interpreterGoal);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "일일 학습 목표가 저장되었습니다.");
            response.put("goal", savedGoal);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "일일 학습 목표 저장에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 현재 주의 일일 학습 목표 조회 (AJAX용)
     */
    @GetMapping(value = "/api/mypage/daily-goals", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDailyGoals(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        Map<String, Object> response = new HashMap<>();
        if (userId == null) {
            response.put("success", false);
            response.put("error", "로그인이 필요한 서비스입니다.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            MyDailyGoalDTO goal = dashboardService.getCurrentDailyGoal(userId);
            response.put("success", true);
            response.put("goal", goal);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "일일 학습 목표 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 수료증 전체 목록 조회 (AJAX용)
     */
    @GetMapping("/api/mypage/certificates")
    @ResponseBody
    public Map<String, Object> getAllCertificates(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        Map<String, Object> response = new HashMap<>();
        if (userId == null) {
            response.put("success", false);
            response.put("error", "로그인이 필요한 서비스입니다.");
            return response;
        }

        try {
            List<MyCertificateDTO> certificates = profileService.getCertificates(userId);
            response.put("success", true);
            response.put("certificates", certificates != null ? certificates : new java.util.ArrayList<>());
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "수료증 목록을 불러오는데 실패했습니다: " + e.getMessage());
            return response;
        }
    }

    /**
     * 완료된 강의에 대해 수료증 발급 체크 (AJAX용)
     */
    @PostMapping("/api/mypage/certificates/check")
    @ResponseBody
    public Map<String, Object> checkAndIssueCertificates(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        Map<String, Object> response = new HashMap<>();
        if (userId == null) {
            response.put("success", false);
            response.put("error", "로그인이 필요한 서비스입니다.");
            return response;
        }

        try {
            // 모든 완료된 강의에 대해 수료증 발급 체크
            courseVideoService.checkAndIssueCertificatesForAllCompletedCourses(userId);
            
            // 수료증 목록 다시 조회
            List<MyCertificateDTO> certificates = profileService.getCertificates(userId);
            response.put("success", true);
            response.put("message", "수료증 발급 체크가 완료되었습니다.");
            response.put("certificates", certificates != null ? certificates : new java.util.ArrayList<>());
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "수료증 발급 체크에 실패했습니다: " + e.getMessage());
            return response;
        }
    }

    /**
     * 특정 날짜의 수강한 강의 목록 조회 (AJAX용)
     */
    @GetMapping(value = "/api/mypage/daily-courses", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDailyCourses(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day,
            HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        Map<String, Object> response = new HashMap<>();
        if (userId == null) {
            response.put("success", false);
            response.put("error", "로그인이 필요한 서비스입니다.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            List<MyDailyCourseDTO> courses = dashboardService.getDailyCourses(userId, year, month, day);
            response.put("success", true);
            response.put("courses", courses != null ? courses : new java.util.ArrayList<>());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "일일 강의 목록을 불러오는데 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
