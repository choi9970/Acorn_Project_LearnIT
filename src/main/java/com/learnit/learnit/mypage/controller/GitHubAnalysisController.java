package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.GitHubAnalysisDTO;
import com.learnit.learnit.mypage.dto.ProfileDTO;
import com.learnit.learnit.mypage.dto.SkillChartDTO;
import com.learnit.learnit.mypage.service.GitHubAnalysisService;
import com.learnit.learnit.mypage.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mypage/github")
@RequiredArgsConstructor
public class GitHubAnalysisController {

    private final GitHubAnalysisService githubAnalysisService;
    private final ProfileService profileService;

    /**
     * 저장된 GitHub 분석 결과 조회 API
     */
    @GetMapping("/analysis")
    public ResponseEntity<Map<String, Object>> getGitHubAnalysis(HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        try {
            GitHubAnalysisDTO analysis = githubAnalysisService.getSavedGitHubAnalysis(userId);
            
            if (analysis == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "저장된 분석 결과가 없습니다.");
                return ResponseEntity.ok(response);
            }
            
            SkillChartDTO skillChart = githubAnalysisService.generateSkillChart(analysis);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysis", analysis);
            response.put("skillChart", skillChart);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "분석 결과 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * GitHub 프로필 분석 및 저장 API
     */
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeGitHub(HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        try {
            // 사용자 프로필 조회
            ProfileDTO profile = profileService.getProfile(userId);
            
            if (profile == null || profile.getGithubUrl() == null || profile.getGithubUrl().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "GitHub URL이 등록되지 않았습니다.");
                return ResponseEntity.badRequest().body(error);
            }

            // GitHub 분석 수행
            GitHubAnalysisDTO analysis = githubAnalysisService.analyzeGitHubProfile(profile.getGithubUrl());
            analysis.setUserId(userId);
            
            // 분석 결과 저장
            githubAnalysisService.saveGitHubAnalysis(userId, analysis);
            
            // 스킬 차트 데이터 생성
            SkillChartDTO skillChart = githubAnalysisService.generateSkillChart(analysis);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysis", analysis);
            response.put("skillChart", skillChart);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            // Rate limit 오류 등 특정 런타임 예외 처리
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            String errorMessage = e.getMessage();
            
            log.error("GitHub 분석 중 런타임 예외 발생: {}", errorMessage, e);
            
            // Rate limit 오류인 경우 더 명확한 메시지
            if (errorMessage != null && (errorMessage.contains("rate limit") || errorMessage.contains("요청 한도"))) {
                error.put("error", errorMessage);
                error.put("errorType", "RATE_LIMIT");
                return ResponseEntity.status(429).body(error); // 429 Too Many Requests
            }
            
            // 서버 오류 (504, 503 등)
            if (errorMessage != null && errorMessage.contains("서버가 일시적으로")) {
                error.put("error", errorMessage);
                error.put("errorType", "SERVER_ERROR");
                return ResponseEntity.status(503).body(error); // 503 Service Unavailable
            }
            
            error.put("error", errorMessage != null ? errorMessage : "GitHub 분석 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "GitHub 분석 중 오류가 발생했습니다: " + e.getMessage());
            log.error("GitHub 분석 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(500).body(error);
        }
    }
}

