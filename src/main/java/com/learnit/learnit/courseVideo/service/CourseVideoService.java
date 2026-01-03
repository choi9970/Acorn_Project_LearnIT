package com.learnit.learnit.courseVideo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnit.learnit.courseVideo.dto.CourseFile;
import com.learnit.learnit.courseVideo.dto.CourseVideo;
import com.learnit.learnit.courseVideo.dto.CurriculumSection;
import com.learnit.learnit.courseVideo.repository.CourseVideoMapper;
import com.learnit.learnit.quiz.dto.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseVideoService {
    private final CourseVideoMapper courseVideoMapper;

    @Value("${rapid-api.key}")
    private String rapidApiKey;

    @Value("${rapid-api.host}")
    private String rapidApiHost;

    @Value("${rapid-api.url}")
    private String judge0Url;

    public CourseVideo getChapterDetail(Long chapterId){
        return courseVideoMapper.findById(chapterId);
    }

    public Long getPrevChapterId(Long courseId, Long currentOrder) {
        return courseVideoMapper.selectPrevChapterId(courseId, currentOrder);
    }

    public Long getNextChapterId(Long courseId, Long currentOrder) {
        return courseVideoMapper.selectNextChapterId(courseId, currentOrder);
    }

    public void saveStudyLog(Long userId, Long courseId, Long chapterId, Integer playTime) {
        courseVideoMapper.insertOrUpdateStudyLog(userId, courseId, chapterId, playTime);
    }

    public int getProgressPercent(Long userId, Long courseId) {
        int total = courseVideoMapper.countTotalChapters(courseId);
        if (total == 0) return 0;
        int completed = courseVideoMapper.countCompletedChapters(userId, courseId);

        return (int) ((double) completed / total * 100);
    }

    public void updateChapterDuration(Long chapterId, int duration) {
        courseVideoMapper.updateChapterDuration(chapterId, duration);
    }

    public List<CurriculumSection> getCurriculumGrouped(Long courseId) {
        List<CourseVideo> allChapters = courseVideoMapper.selectChapterList(courseId);

        Map<String, List<CourseVideo>> grouped = allChapters.stream()
                .collect(Collectors.groupingBy(
                        ch -> ch.getSectionTitle() == null ? "기타" : ch.getSectionTitle(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CurriculumSection> result = new ArrayList<>();
        for (Map.Entry<String, List<CourseVideo>> entry : grouped.entrySet()) {
            result.add(new CurriculumSection(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    public List<CourseFile> getCourseResources(Long courseId) {
        return courseVideoMapper.selectCourseResources(courseId);
    }

    public Long getNextQuizId(CourseVideo currentChapter, Long nextChapterId, Map<String, Quiz> quizMap) {

        if (!quizMap.containsKey(currentChapter.getSectionTitle())) {
            return null;
        }
        Quiz sectionQuiz = quizMap.get(currentChapter.getSectionTitle());

        if (nextChapterId == null) {
            return sectionQuiz.getQuizId();
        }

        CourseVideo nextChapter = this.getChapterDetail(nextChapterId);

        if (nextChapter != null && !nextChapter.getSectionTitle().equals(currentChapter.getSectionTitle())) {
            return sectionQuiz.getQuizId();
        }

        return null;
    }

    public boolean isUserEnrolled(Long userId, Long courseId) {
        String status = courseVideoMapper.selectEnrollmentStatus(userId, courseId);

        return "ACTIVE".equals(status);
    }

    public Map<String, Object> runInterpreterCode(String sourceCode, String languageId) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 변수(rapidApiKey, rapidApiHost)를 사용
        headers.set("X-RapidAPI-Key", rapidApiKey);
        headers.set("X-RapidAPI-Host", rapidApiHost);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("source_code", sourceCode);
        requestBody.put("language_id", Integer.parseInt(languageId));

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBody);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            // 변수(judge0Url) 사용
            ResponseEntity<Map> response = restTemplate.postForEntity(judge0Url, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> result = new HashMap<>();

            if (responseBody != null) {
                String stdout = (String) responseBody.get("stdout");
                String stderr = (String) responseBody.get("stderr");
                String compileOutput = (String) responseBody.get("compile_output");

                if (stdout != null) result.put("output", stdout);
                else if (stderr != null) result.put("output", "에러 발생:\n" + stderr);
                else if (compileOutput != null) result.put("output", "컴파일 에러:\n" + compileOutput);
                else result.put("output", "실행 완료 (출력값 없음)");
            } else {
                result.put("output", "결과가 없습니다.");
            }
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("output", "서버 오류: " + e.getMessage());
            return error;
        }
    }
}
