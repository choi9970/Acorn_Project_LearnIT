package com.learnit.learnit.courseVideo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnit.learnit.courseVideo.dto.CourseFile;
import com.learnit.learnit.courseVideo.dto.CourseVideo;
import com.learnit.learnit.courseVideo.dto.CurriculumSection;
import com.learnit.learnit.courseVideo.repository.CourseVideoMapper;
import com.learnit.learnit.mypage.mapper.MyCertificateMapper;
import com.learnit.learnit.quiz.dto.Quiz;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseVideoService {
    private final CourseVideoMapper courseVideoMapper;
    private final MyCertificateMapper certificateMapper;

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

    @Transactional
    public void saveStudyLog(Long userId, Long courseId, Long chapterId, Integer playTime) {
        // 학습 로그 저장
        courseVideoMapper.insertOrUpdateStudyLog(userId, courseId, chapterId, playTime);
        
        // 완강 여부 체크 및 수료증 자동 발급
        checkAndIssueCertificate(userId, courseId);
    }
    
    /**
     * 완강 여부 체크 및 수료증 자동 발급 (public 메서드 - API에서 호출 가능)
     */
    public void checkAndIssueCertificateForUser(Long userId, Long courseId) {
        checkAndIssueCertificate(userId, courseId);
    }
    
    /**
     * 사용자의 모든 완료된 강의에 대해 수료증 발급 체크
     */
    public void checkAndIssueCertificatesForAllCompletedCourses(Long userId) {
        try {
            List<Long> enrolledCourseIds = courseVideoMapper.selectEnrolledCourseIds(userId);
            
            if (enrolledCourseIds.isEmpty()) {
                return;
            }
            
            for (Long courseId : enrolledCourseIds) {
                // 챕터가 있는 강의만 체크
                int totalChapters = courseVideoMapper.countTotalChaptersWithDuration(courseId);
                if (totalChapters == 0) {
                    int allChapters = courseVideoMapper.countTotalChapters(courseId);
                    if (allChapters == 0) {
                        continue;
                    }
                    // duration이 없는 챕터만 있는 경우도 체크
                    totalChapters = allChapters;
                }
                
                // 수료증 발급 체크
                checkAndIssueCertificateWithResult(userId, courseId);
            }
        } catch (Exception e) {
            log.error("Error checking certificates for all courses: userId={}", userId, e);
        }
    }
    
    /**
     * 수료증 발급 체크 (발급 결과 반환)
     * @return 0: 발급 안됨, 1: 새로 발급됨, 2: 이미 발급됨
     */
    private int checkAndIssueCertificateWithResult(Long userId, Long courseId) {
        try {
            // 전체 챕터 수 확인 (duration_sec > 0인 챕터만 카운트)
            int totalChapters = courseVideoMapper.countTotalChaptersWithDuration(courseId);
            log.info("Certificate check - totalChapters (with duration): {}, userId: {}, courseId: {}", 
                totalChapters, userId, courseId);
            
            if (totalChapters == 0) {
                log.warn("No chapters with duration found for courseId: {}", courseId);
                // duration이 없는 챕터만 있는 경우, 전체 챕터 수로 다시 확인
                int allChapters = courseVideoMapper.countTotalChapters(courseId);
                if (allChapters == 0) {
                    log.warn("No chapters found at all for courseId: {}", courseId);
                    return 0;
                }
                // duration이 없는 챕터만 있는 경우, 전체 챕터 수로 진행
                totalChapters = allChapters;
                log.info("Using all chapters (no duration): {}", totalChapters);
            }
            
            // 완료한 챕터 수 확인 (95% 이상 수강한 챕터)
            int completedChapters = courseVideoMapper.countCompletedChapters(userId, courseId);
            log.info("Certificate check - completedChapters: {}, totalChapters: {}, userId: {}, courseId: {}", 
                completedChapters, totalChapters, userId, courseId);
            
            // 모든 챕터를 완료했는지 확인 (완료 챕터 수가 전체 챕터 수 이상이면 완료)
            // 단, totalChapters가 0이면 완료로 간주하지 않음
            boolean conditionMet = totalChapters > 0 && completedChapters >= totalChapters;
            
            if (conditionMet) {
                // enrollment_id 조회
                Long enrollmentId = courseVideoMapper.selectEnrollmentId(userId, courseId);
                
                if (enrollmentId == null) {
                    return 0;
                }
                
                // 이미 수료증이 발급되었는지 확인
                boolean exists = courseVideoMapper.existsCertificate(enrollmentId);
                
                if (exists) {
                    return 2; // 이미 발급됨
                }
                
                // enrollment 완강 처리
                courseVideoMapper.updateEnrollmentCompleted(enrollmentId);
                
                // 수료증 자동 생성
                createCertificate(enrollmentId, userId, courseId);
                
                return 1; // 새로 발급됨
            } else {
                return 0; // 발급 안됨
            }
        } catch (Exception e) {
            log.error("Error checking completion and issuing certificate: userId={}, courseId={}", 
                userId, courseId, e);
            return 0;
        }
    }
    
    
    /**
     * 완강 여부 체크 및 수료증 자동 발급
     */
    private void checkAndIssueCertificate(Long userId, Long courseId) {
        checkAndIssueCertificateWithResult(userId, courseId);
    }
    
    /**
     * 수료증 생성
     */
    private void createCertificate(Long enrollmentId, Long userId, Long courseId) {
        // 수료증 번호 생성 (CERT-YYYYMMDD-ENROLLMENT_ID 형식)
        String certificateNumber = "CERT-" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
            "-" + enrollmentId;
        
        certificateMapper.insertCertificate(enrollmentId, certificateNumber);
        log.info("Certificate created: enrollmentId={}, certificateNumber={}", 
            enrollmentId, certificateNumber);
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

    public boolean isInstructor(Long userId, Long courseId) {
        Long instructorId = courseVideoMapper.selectCourseInstructorId(courseId);
        return userId != null && userId.equals(instructorId);
    }

    public void saveInterpreterLog(Long userId, Long courseId, Long chapterId, Integer languageId) {
        courseVideoMapper.insertInterpreterLog(userId, courseId, chapterId, languageId);
    }

    public Long getFirstChapterId(Long courseId) {
        return courseVideoMapper.selectFirstChapterId(courseId);
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
            log.error("Error running interpreter code", e);
            Map<String, Object> error = new HashMap<>();
            error.put("output", "서버 오류: " + e.getMessage());
            return error;
        }
    }
}
