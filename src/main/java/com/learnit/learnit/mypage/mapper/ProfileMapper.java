package com.learnit.learnit.mypage.mapper;

import com.learnit.learnit.mypage.dto.GitHubAnalysisDTO;
import com.learnit.learnit.mypage.dto.ProfileDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProfileMapper {
    /**
     * 사용자 프로필 조회
     */
    ProfileDTO selectProfileByUserId(@Param("userId") Long userId);
    
    /**
     * 개인정보 수정
     */
    void updateProfile(@Param("userId") Long userId, 
                       @Param("name") String name,
                       @Param("password") String password,
                       @Param("email") String email,
                       @Param("phone") String phone,
                       @Param("githubUrl") String githubUrl,
                       @Param("region") String region,
                       @Param("profileImg") String profileImg);
    
    /**
     * 프로필 이미지 업데이트
     */
    void updateProfileImage(@Param("userId") Long userId, 
                            @Param("profileImg") String profileImg);
    
    /**
     * GitHub 분석 결과 조회
     */
    GitHubAnalysisDTO selectGitHubAnalysisByUserId(@Param("userId") Long userId);
    
    /**
     * GitHub 분석 결과 저장/업데이트
     */
    void upsertGitHubAnalysis(
        @Param("userId") Long userId,
        @Param("githubUsername") String githubUsername,
        @Param("totalRepos") Integer totalRepos,
        @Param("totalCommits") Integer totalCommits,
        @Param("mostUsedLanguage") String mostUsedLanguage,
        @Param("languageStatsJson") String languageStatsJson,
        @Param("skillLevelsJson") String skillLevelsJson,
        @Param("analysisDate") String analysisDate
    );
}

