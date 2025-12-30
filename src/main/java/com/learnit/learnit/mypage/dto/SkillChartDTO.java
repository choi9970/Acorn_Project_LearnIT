package com.learnit.learnit.mypage.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SkillChartDTO {
    private Map<String, Double> skills; // 기술명: 레벨 (0-100)
    private String[] skillNames; // 차트에 표시할 기술명 배열
    private Double[] skillLevels; // 차트에 표시할 레벨 배열
}

