package com.learnit.learnit.courseVideo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnit.learnit.courseVideo.service.CourseVideoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InterpreterController {

    private final CourseVideoService courseVideoService;
    private static final String RAPID_API_KEY = "ba94e9a805msh82fa76cff1b1842p192b67jsn9397668b1906";
    private static final String JUDGE0_URL = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=false&wait=true";

    @PostMapping("/api/interpreter/run")
    public Map<String, Object> runCode(@RequestBody Map<String, String> payload, HttpSession session) {
        System.out.println("========== [1] 코드 실행 요청 도착 ==========");
        String sourceCode = payload.get("code");
        String languageId = payload.get("languageId");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-RapidAPI-Key", RAPID_API_KEY);
        headers.set("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("source_code", sourceCode);
        requestBody.put("language_id", Integer.parseInt(languageId));

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBody);


            // 문자열(jsonBody)을 담아서 보냅니다.
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(JUDGE0_URL, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> result = new HashMap<>();

            if (responseBody != null) {
                String stdout = (String) responseBody.get("stdout");
                String stderr = (String) responseBody.get("stderr");
                String compileOutput = (String) responseBody.get("compile_output"); // 컴파일 에러

                if (stdout != null) {
                    result.put("output", stdout);
                } else if (stderr != null) {
                    result.put("output", "에러 발생:\n" + stderr);
                } else if (compileOutput != null) {
                    result.put("output", "컴파일/문법 에러:\n" + compileOutput);
                } else {
                    result.put("output", "실행 완료 (출력값 없음)");
                }

                // 인터프리터 실행 로그 저장 (성공한 경우만)
                if (stdout != null || (stderr == null && compileOutput == null)) {
                    Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
                    String courseIdStr = payload.get("courseId");
                    String chapterIdStr = payload.get("chapterId");

                    if (userId != null && courseIdStr != null && chapterIdStr != null) {
                        try {
                            Long courseId = Long.parseLong(courseIdStr);
                            Long chapterId = Long.parseLong(chapterIdStr);
                            Integer langId = Integer.parseInt(languageId);
                            courseVideoService.saveInterpreterLog(userId, courseId, chapterId, langId);
                            System.out.println("인터프리터 실행 로그 저장 성공: userId=" + userId + ", courseId=" + courseId + ", chapterId=" + chapterId);
                        } catch (NumberFormatException e) {
                            System.err.println("ID 파싱 오류: " + e.getMessage());
                        } catch (Exception e) {
                            System.err.println("인터프리터 실행 로그 저장 실패: " + e.getMessage());
                        }
                    }
                }
            } else {
                result.put("output", "결과가 비어있습니다.");
            }
            return result;

        } catch (HttpClientErrorException e) {

            Map<String, Object> error = new HashMap<>();
            error.put("output", "실행 실패 (" + e.getStatusCode() + "):\n" + e.getResponseBodyAsString());
            return error;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("output", "서버 내부 오류: " + e.getMessage());
            return error;
        }
    }
}