package com.learnit.learnit.mypage.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GitHubAnalysisDTO {
    private Long analysisId;
    private Long userId;
    private String username;
    private Integer totalRepos;
    private Integer totalCommits;
    private Map<String, Integer> languageStats; // 언어별 바이트 수
    private Map<String, Double> skillLevels; // 언어별 스킬 레벨 (0-100)
    private String mostUsedLanguage;
    private String analysisDate;
    // JSON 문자열로 저장된 데이터 (DB 조회용)
    private String languageStatsJson;
    private String skillLevelsJson;
}

