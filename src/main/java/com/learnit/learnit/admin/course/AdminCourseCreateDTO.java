package com.learnit.learnit.admin.course;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class AdminCourseCreateDTO {
    private Long courseId; // 생성된 ID 반환용
    private String title;
    private String description;
    private Long categoryId;
    private int price;
    private List<Long> instructorIds; // UI에서 여러 명 선택 가능
    
    // 화면 표시용 (수정 폼 등)
    private String instructorName;
    private String instructorNickname;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private boolean alwaysOpen;

    // 파일 업로드 (입력)
    private MultipartFile thumbnail;
    private MultipartFile detailThumbnail;

    // 파일 경로 (DB 저장용)
    private String thumbnailUrl;
    private String detailImgUrl;
    
    // 화면 표시용 파일명
    private String thumbnailFileName;
    private String detailThumbnailFileName;

    // 커리큘럼 (섹션 + 챕터)
    private List<SectionRequest> sections;
    
    // 파이널 퀴즈
    private QuizRequest finalQuiz;

    @Data
    public static class SectionRequest {
        private String title;
        private List<ChapterRequest> chapters;
        private List<QuizRequest> quizzes;
    }

    @Data
    public static class ChapterRequest {
        private Long chapterId; // 수정 시 식별용
        private String title;
        private String videoUrl;
        private MultipartFile file; // 챕터 자료 파일
        
        // 기존 파일 정보 (수정 시 사용)
        private String existingFileUrl;
        private String existingFileName;
    }
    
    @Data
    public static class QuizRequest {
        private Long quizId;
        private String title;
        private List<QuestionRequest> questions;
    }

    @Data
    public static class QuestionRequest {
        private Long questionId;
        private String content;
        private String explanation;
        private List<OptionRequest> options;
    }

    @Data
    public static class OptionRequest {
        private Long optionId;
        private String content;
        private String isCorrect; // "Y" or "N"
    }
}
