package com.learnit.learnit.mypage.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.learnit.learnit.mypage.dto.GitHubAnalysisDTO;
import com.learnit.learnit.mypage.dto.SkillChartDTO;
import com.learnit.learnit.mypage.mapper.ProfileMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProfileMapper profileMapper;
    private static final String GITHUB_API_BASE = "https://api.github.com";
    
    // 주요 프로그래밍 언어 목록 (필터링용) - 순수 프로그래밍 언어만
    private static final Set<String> MAJOR_PROGRAMMING_LANGUAGES = Set.of(
        // Top 인기 언어
        "Java", "JavaScript", "TypeScript", "Python", "C", "C++", "C#", 
        "Go", "Rust", "Swift", "Kotlin", "PHP", "Ruby", "Scala", "R",
        // 추가 언어
        "Dart", "Lua", "Perl", "Objective-C", "MATLAB", "Groovy", "Clojure",
        "Elixir", "Erlang", "Haskell", "F#", "VB.NET", "Solidity", "Assembly",
        // 스크립팅 언어
        "Shell", "Bash"
    );

    /**
     * 주요 프로그래밍 언어인지 확인
     */
    private boolean isMajorProgrammingLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return false;
        }
        return MAJOR_PROGRAMMING_LANGUAGES.contains(language);
    }

    /**
     * GitHub URL에서 username 추출
     */
    public String extractUsername(String githubUrl) {
        if (githubUrl == null || githubUrl.trim().isEmpty()) {
            return null;
        }

        String url = githubUrl.trim();
        
        // https://github.com/username 또는 github.com/username 형식 처리
        if (url.contains("github.com/")) {
            String[] parts = url.split("github.com/");
            if (parts.length > 1) {
                String username = parts[1].split("/")[0].split("\\?")[0];
                return username.trim();
            }
        }
        
        return null;
    }

    /**
     * GitHub 프로필 분석
     */
    public GitHubAnalysisDTO analyzeGitHubProfile(String githubUrl) {
        String username = extractUsername(githubUrl);
        
        if (username == null) {
            throw new IllegalArgumentException("유효한 GitHub URL이 아닙니다.");
        }

        try {
            GitHubAnalysisDTO analysis = new GitHubAnalysisDTO();
            analysis.setUsername(username);
            analysis.setAnalysisDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 사용자 정보 조회
            String userUrl = GITHUB_API_BASE + "/users/" + username;
            log.info("GitHub API 요청 시작: {}", userUrl);
            ResponseEntity<String> userResponse = null;
            try {
                userResponse = restTemplate.getForEntity(userUrl, String.class);
                log.info("GitHub API 응답 상태: {}", userResponse.getStatusCode());
                
                if (userResponse.getStatusCode().is2xxSuccessful()) {
                    JsonNode userNode = objectMapper.readTree(userResponse.getBody());
                    analysis.setTotalRepos(userNode.get("public_repos") != null ? userNode.get("public_repos").asInt() : 0);
                }
            } catch (ResourceAccessException e) {
                log.error("GitHub API 연결 타임아웃: {}", userUrl, e);
                throw new RuntimeException("GitHub API 연결이 타임아웃되었습니다. 잠시 후 다시 시도해주세요.", e);
            } catch (HttpClientErrorException e) {
                log.error("GitHub API 클라이언트 오류 (사용자 정보 조회): 상태코드={}, 응답={}", e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.warn("GitHub 사용자를 찾을 수 없음: {}", username);
                    throw new IllegalArgumentException("GitHub 사용자를 찾을 수 없습니다: " + username);
                } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    String responseBody = e.getResponseBodyAsString();
                    log.error("GitHub API 403 Forbidden 응답: {}", responseBody);
                    if (responseBody != null && responseBody.contains("rate limit exceeded")) {
                        log.error("GitHub API rate limit 초과: {}", username);
                        throw new RuntimeException("GitHub API 요청 한도가 초과되었습니다. 잠시 후 다시 시도해주세요. (인증된 요청을 사용하면 더 높은 한도를 사용할 수 있습니다.)", e);
                    } else {
                        // 403 오류지만 rate limit이 아닌 경우 (권한 문제 등)
                        log.error("GitHub API 403 오류 (rate limit 아님): {}", responseBody);
                        throw new RuntimeException("GitHub API 접근이 거부되었습니다. 토큰 권한을 확인해주세요. 응답: " + (responseBody != null ? responseBody.substring(0, Math.min(200, responseBody.length())) : "알 수 없음"), e);
                    }
                } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    log.error("GitHub API 401 Unauthorized: 토큰이 유효하지 않거나 만료되었습니다.");
                    throw new RuntimeException("GitHub API 인증에 실패했습니다. 토큰이 유효한지 확인해주세요.", e);
                }
                throw e;
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    log.error("GitHub API 서버 오류 (504/503): {}", userUrl, e);
                    throw new RuntimeException("GitHub 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.", e);
                }
                throw e;
            }

            // 레포지토리 목록 조회 (최대 100개) - 개인 레포지토리와 fork된 레포지토리 모두 포함
            Map<String, Integer> languageStats = new HashMap<>();
            int totalCommits = 0;

            // 개인 레포지토리 조회
            String reposUrl = GITHUB_API_BASE + "/users/" + username + "/repos?per_page=100&sort=updated&type=all";
            ResponseEntity<String> reposResponse = null;
            
            try {
                reposResponse = restTemplate.getForEntity(reposUrl, String.class);
            } catch (ResourceAccessException e) {
                log.error("GitHub API 연결 타임아웃: {}", reposUrl, e);
                throw new RuntimeException("GitHub API 연결이 타임아웃되었습니다. 잠시 후 다시 시도해주세요.", e);
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    log.error("GitHub API 서버 오류 (504/503): {}", reposUrl, e);
                    throw new RuntimeException("GitHub 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.", e);
                }
                throw e;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.warn("GitHub 사용자를 찾을 수 없음: {}", username);
                    throw new IllegalArgumentException("GitHub 사용자를 찾을 수 없습니다: " + username);
                } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    String responseBody = e.getResponseBodyAsString();
                    if (responseBody != null && responseBody.contains("rate limit exceeded")) {
                        log.error("GitHub API rate limit 초과: {}", username);
                        throw new RuntimeException("GitHub API 요청 한도가 초과되었습니다. 잠시 후 다시 시도해주세요. (인증된 요청을 사용하면 더 높은 한도를 사용할 수 있습니다.)", e);
                    }
                }
                throw e;
            }
            
            if (reposResponse != null && reposResponse.getStatusCode().is2xxSuccessful()) {
                try {
                    JsonNode reposArray = objectMapper.readTree(reposResponse.getBody());
                    
                    if (reposArray.isArray()) {
                        // 실제 레포지토리 배열 크기로 totalRepos 업데이트 (더 정확함)
                        int actualReposCount = reposArray.size();
                        int previousReposCount = analysis.getTotalRepos();
                        if (actualReposCount > 0) {
                            analysis.setTotalRepos(actualReposCount);
                            log.info("실제 레포지토리 수로 업데이트: {}개 (이전 값: {}개)", 
                                actualReposCount, previousReposCount);
                        }
                        
                        // 전체 레포지토리 분석 (Personal Access Token 사용 시 Rate Limit 충분)
                        // 레포지토리 목록 조회 시 per_page=100으로 설정되어 있으므로 최대 100개까지 분석 가능
                        int maxRepos = reposArray.size();
                        
                        for (int i = 0; i < maxRepos; i++) {
                            JsonNode repo = reposArray.get(i);
                            
                            // 각 레포지토리의 언어 정보 및 커밋 수 조회
                            String repoName = repo.get("name").asText();
                            String repoOwner = repo.get("owner").get("login").asText();
                            
                            try {
                                // 언어 정보 조회
                                String languagesUrl = GITHUB_API_BASE + "/repos/" + repoOwner + "/" + repoName + "/languages";
                                ResponseEntity<String> languagesResponse = restTemplate.getForEntity(languagesUrl, String.class);
                                
                                if (languagesResponse.getStatusCode().is2xxSuccessful()) {
                                    JsonNode languagesNode = objectMapper.readTree(languagesResponse.getBody());
                                    Iterator<String> fieldNames = languagesNode.fieldNames();
                                    while (fieldNames.hasNext()) {
                                        String language = fieldNames.next();
                                        // 주요 프로그래밍 언어만 필터링
                                        if (isMajorProgrammingLanguage(language)) {
                                            int bytes = languagesNode.get(language).asInt();
                                            languageStats.put(language, languageStats.getOrDefault(language, 0) + bytes);
                                        }
                                    }
                                }
                                
                                // 커밋 수 조회 (per_page=1로 첫 페이지만 가져와서 Link 헤더에서 총 수 추정)
                                String commitsUrl = GITHUB_API_BASE + "/repos/" + repoOwner + "/" + repoName + "/commits?per_page=1&author=" + username;
                                try {
                                    ResponseEntity<String> commitsResponse = restTemplate.getForEntity(commitsUrl, String.class);
                                    
                                    if (commitsResponse.getStatusCode().is2xxSuccessful()) {
                                        // Link 헤더에서 마지막 페이지 번호 추출
                                        String linkHeader = commitsResponse.getHeaders().getFirst("Link");
                                        int repoCommits = 0;
                                        
                                        if (linkHeader != null && linkHeader.contains("rel=\"last\"")) {
                                            try {
                                                // Link 헤더 형식 예시:
                                                // <https://api.github.com/repos/owner/repo/commits?per_page=1&author=user&page=2>; rel="next", <https://api.github.com/repos/owner/repo/commits?per_page=1&author=user&page=5>; rel="last"
                                                
                                                // "rel=\"last\"" 앞의 URL에서 page 번호 추출
                                                int lastIndex = linkHeader.indexOf("rel=\"last\"");
                                                if (lastIndex > 0) {
                                                    String beforeLast = linkHeader.substring(0, lastIndex);
                                                    
                                                    // page= 또는 &page= 패턴 찾기 (뒤에서부터)
                                                    int pageIndex = beforeLast.lastIndexOf("&page=");
                                                    if (pageIndex == -1) {
                                                        pageIndex = beforeLast.lastIndexOf("page=");
                                                        if (pageIndex != -1) {
                                                            pageIndex += 5; // "page=" 길이
                                                        }
                                                    } else {
                                                        pageIndex += 6; // "&page=" 길이
                                                    }
                                                    
                                                    if (pageIndex > 0 && pageIndex < beforeLast.length()) {
                                                        String pageStr = beforeLast.substring(pageIndex);
                                                        // > 또는 ; 또는 & 또는 공백까지 추출
                                                        pageStr = pageStr.split("[>;&\\s]")[0].trim();
                                                        if (!pageStr.isEmpty()) {
                                                            repoCommits = Integer.parseInt(pageStr);
                                                            log.debug("레포지토리 {}/{} 커밋 수 (Link 헤더): {}", repoOwner, repoName, repoCommits);
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {
                                                log.debug("Link 헤더 파싱 실패: {}/{} - Link: {}", repoOwner, repoName, linkHeader, e);
                                            }
                                        } else {
                                            // Link 헤더가 없으면 커밋이 1개 이하 (또는 페이지네이션 없음)
                                            log.debug("레포지토리 {}/{} Link 헤더 없음 (커밋 1개 이하 또는 페이지네이션 없음)", repoOwner, repoName);
                                        }
                                        
                                        // Link 헤더 파싱 실패 또는 Link 헤더가 없는 경우 응답 본문 확인
                                        if (repoCommits == 0) {
                                            JsonNode commitsArray = objectMapper.readTree(commitsResponse.getBody());
                                            if (commitsArray.isArray() && commitsArray.size() > 0) {
                                                repoCommits = 1; // 최소 1개 커밋
                                                log.debug("레포지토리 {}/{} 커밋 수 (응답 본문 추정): {}", repoOwner, repoName, repoCommits);
                                            }
                                        }
                                        
                                        totalCommits += repoCommits;
                                        log.debug("레포지토리 {}/{} 최종 커밋 수: {}, 누적 총 커밋 수: {}", repoOwner, repoName, repoCommits, totalCommits);
                                    }
                                } catch (Exception e) {
                                    log.warn("레포지토리 커밋 수 조회 실패: {}/{}", repoOwner, repoName, e);
                                    // 커밋 수 조회 실패해도 계속 진행
                                }
                                
                                // API Rate Limit 방지를 위한 대기 (rate limit 오류 시 대기 시간 증가)
                                Thread.sleep(500);
                            } catch (ResourceAccessException e) {
                                log.warn("레포지토리 정보 조회 타임아웃: {}/{}", repoOwner, repoName);
                                // 타임아웃 발생 시 해당 레포지토리는 건너뛰고 계속 진행
                                continue;
                            } catch (HttpClientErrorException e) {
                                if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                                    String responseBody = e.getResponseBodyAsString();
                                    if (responseBody != null && responseBody.contains("rate limit exceeded")) {
                                        log.error("GitHub API rate limit 초과: 레포지토리 정보 조회 중");
                                        // Rate limit 초과 시 전체 분석 중단
                                        throw new RuntimeException("GitHub API 요청 한도가 초과되었습니다. 잠시 후 다시 시도해주세요. (인증된 요청을 사용하면 더 높은 한도를 사용할 수 있습니다.)", e);
                                    }
                                }
                                log.warn("레포지토리 정보 조회 실패: {}/{}", repoOwner, repoName, e);
                                // 기타 403 오류는 건너뛰고 계속 진행
                                continue;
                            } catch (Exception e) {
                                log.warn("레포지토리 정보 조회 실패: {}/{}", repoOwner, repoName, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("레포지토리 데이터 파싱 실패: {}", username, e);
                    // 파싱 실패해도 기본 정보는 반환
                }
            }

            // 전체 레포지토리 수는 사용자 정보에서 가져온 값 사용
            analysis.setTotalCommits(totalCommits);
            analysis.setLanguageStats(languageStats);

            // 스킬 레벨 계산 (언어별 사용량을 0-100 스케일로 변환)
            Map<String, Double> skillLevels = calculateSkillLevels(languageStats);
            analysis.setSkillLevels(skillLevels);

            // 가장 많이 사용한 언어
            if (!languageStats.isEmpty()) {
                String mostUsed = languageStats.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                analysis.setMostUsedLanguage(mostUsed);
            }

            return analysis;
        } catch (IllegalArgumentException e) {
            // 사용자 입력 오류는 그대로 전달
            throw e;
        } catch (RuntimeException e) {
            // 이미 처리된 런타임 예외는 그대로 전달
            throw e;
        } catch (Exception e) {
            log.error("GitHub 프로필 분석 실패: {}", username, e);
            throw new RuntimeException("GitHub 프로필 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 언어별 사용량을 스킬 레벨(0-100)로 변환
     */
    private Map<String, Double> calculateSkillLevels(Map<String, Integer> languageStats) {
        if (languageStats.isEmpty()) {
            return new HashMap<>();
        }

        // 최대값 찾기
        int maxBytes = languageStats.values().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(1);

        // 각 언어를 0-100 스케일로 변환
        Map<String, Double> skillLevels = new HashMap<>();
        languageStats.forEach((language, bytes) -> {
            double level = (bytes.doubleValue() / maxBytes) * 100;
            skillLevels.put(language, Math.round(level * 10.0) / 10.0); // 소수점 1자리
        });

        return skillLevels;
    }

    /**
     * 스킬 차트 데이터 생성 (항상 6개 꼭짓점으로 고정)
     */
    public SkillChartDTO generateSkillChart(GitHubAnalysisDTO analysis) {
        SkillChartDTO chart = new SkillChartDTO();
        
        Map<String, Double> skills = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        List<Double> levels = new ArrayList<>();
        
        // 기본 6개 항목 (데이터가 없어도 육각형 모양 유지)
        final int TARGET_COUNT = 6;
        
        if (analysis.getSkillLevels() != null && !analysis.getSkillLevels().isEmpty()) {
            log.info("차트 생성 시작 - 전체 skillLevels: {}", analysis.getSkillLevels());

            // 상위 6개 언어 선택
            List<Map.Entry<String, Double>> topSkills = analysis.getSkillLevels().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TARGET_COUNT)
                .collect(Collectors.toList());

            log.info("선택된 상위 언어: {}", topSkills.stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", ")));

            // 실제 데이터 추가
            for (Map.Entry<String, Double> entry : topSkills) {
                skills.put(entry.getKey(), entry.getValue());
                names.add(entry.getKey());
                levels.add(entry.getValue());
            }
        }

        // 항상 6개가 되도록 빈 항목으로 채우기
        while (names.size() < TARGET_COUNT) {
            String emptyLabel = "-";
            names.add(emptyLabel);
            levels.add(0.0);
            skills.put(emptyLabel, 0.0);
        }

        chart.setSkills(skills);
        chart.setSkillNames(names.toArray(new String[0]));
        chart.setSkillLevels(levels.toArray(new Double[0]));

        log.info("차트 데이터 생성 완료 (항상 6개 고정) - skillNames: {}, skillLevels: {}", 
            Arrays.toString(chart.getSkillNames()), Arrays.toString(chart.getSkillLevels()));

        return chart;
    }

    /**
     * GitHub 분석 결과 저장
     */
    @Transactional
    public void saveGitHubAnalysis(Long userId, GitHubAnalysisDTO analysis) {
        try {
            // Map을 JSON 문자열로 변환
            String languageStatsJson = null;
            String skillLevelsJson = null;
            
            if (analysis.getLanguageStats() != null && !analysis.getLanguageStats().isEmpty()) {
                languageStatsJson = objectMapper.writeValueAsString(analysis.getLanguageStats());
            }
            
            if (analysis.getSkillLevels() != null && !analysis.getSkillLevels().isEmpty()) {
                skillLevelsJson = objectMapper.writeValueAsString(analysis.getSkillLevels());
            }
            
            profileMapper.upsertGitHubAnalysis(
                userId,
                analysis.getUsername(),
                analysis.getTotalRepos() != null ? analysis.getTotalRepos() : 0,
                analysis.getTotalCommits() != null ? analysis.getTotalCommits() : 0,
                analysis.getMostUsedLanguage(),
                languageStatsJson,
                skillLevelsJson,
                analysis.getAnalysisDate()
            );
            
            log.info("GitHub 분석 결과 저장 완료: userId={}, username={}", userId, analysis.getUsername());
        } catch (Exception e) {
            log.error("GitHub 분석 결과 저장 실패: userId={}", userId, e);
            throw new RuntimeException("GitHub 분석 결과 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 저장된 GitHub 분석 결과 조회
     */
    public GitHubAnalysisDTO getSavedGitHubAnalysis(Long userId) {
        try {
            GitHubAnalysisDTO analysis = profileMapper.selectGitHubAnalysisByUserId(userId);
            
            if (analysis == null) {
                return null;
            }
            
            // JSON 문자열을 Map으로 변환
            if (analysis.getLanguageStatsJson() != null && !analysis.getLanguageStatsJson().isEmpty()) {
                try {
                    Map<String, Integer> languageStats = objectMapper.readValue(
                        analysis.getLanguageStatsJson(), 
                        new TypeReference<Map<String, Integer>>() {}
                    );
                    analysis.setLanguageStats(languageStats);
                    log.debug("파싱된 languageStats: {}", languageStats);
                } catch (Exception e) {
                    log.warn("languageStats JSON 파싱 실패: userId={}", userId, e);
                }
            }
            
            if (analysis.getSkillLevelsJson() != null && !analysis.getSkillLevelsJson().isEmpty()) {
                try {
                    log.info("원본 skillLevelsJson: {}", analysis.getSkillLevelsJson());
                    Map<String, Double> skillLevels = objectMapper.readValue(
                        analysis.getSkillLevelsJson(),
                        new TypeReference<Map<String, Double>>() {}
                    );
                    analysis.setSkillLevels(skillLevels);
                    log.info("파싱된 skillLevels (총 {}개): {}", skillLevels.size(), skillLevels);
                    log.info("CSS 포함 여부: {}", skillLevels.containsKey("CSS"));
                    if (skillLevels.containsKey("CSS")) {
                        log.info("CSS 값: {}", skillLevels.get("CSS"));
                    }
                } catch (Exception e) {
                    log.warn("skillLevels JSON 파싱 실패: userId={}", userId, e);
                }
            }
            
            return analysis;
        } catch (Exception e) {
            log.error("GitHub 분석 결과 조회 실패: userId={}", userId, e);
            return null;
        }
    }
}

