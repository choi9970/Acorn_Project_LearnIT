package com.learnit.learnit.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // 🔥 이거 추가됨
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
public class InterpreterController {

    private static final String RAPID_API_KEY = "ba94e9a805msh82fa76cff1b1842p192b67jsn9397668b1906";
    private static final String JUDGE0_URL = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=false&wait=true";

    @PostMapping("/api/interpreter/run")
    public Map<String, Object> runCode(@RequestBody Map<String, String> payload) {
        System.out.println("========== [1] 코드 실행 요청 도착 ==========");
        String sourceCode = payload.get("code");
        String languageId = payload.get("languageId");

        System.out.println("언어 ID: " + languageId);
        // 코드 내용은 너무 길 수 있으니 앞부분만 로그 출력
        System.out.println("소스 코드(일부): " + (sourceCode.length() > 50 ? sourceCode.substring(0, 50) + "..." : sourceCode));

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

            System.out.println("========== [2] 생성된 JSON 데이터 확인 ==========");
            System.out.println(jsonBody); // 이 로그가 정상적으로 나오는지 확인하세요!

            // 문자열(jsonBody)을 담아서 보냅니다.
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            System.out.println("========== [3] Judge0 API 호출 시도 ==========");
            ResponseEntity<Map> response = restTemplate.postForEntity(JUDGE0_URL, entity, Map.class);

            System.out.println("========== [4] Judge0 API 응답 성공 ==========");
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
            } else {
                result.put("output", "결과가 비어있습니다.");
            }
            return result;

        } catch (HttpClientErrorException e) {
            System.out.println("========== [ERROR] API 호출 실패 (클라이언트 에러) ==========");
            System.out.println("상태 코드: " + e.getStatusCode());
            System.out.println("응답 본문: " + e.getResponseBodyAsString());

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