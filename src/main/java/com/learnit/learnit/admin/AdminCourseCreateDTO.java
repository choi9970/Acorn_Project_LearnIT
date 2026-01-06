package com.learnit.learnit.admin;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

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
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private boolean alwaysOpen;

    // 파일 업로드 (입력)
    private org.springframework.web.multipart.MultipartFile thumbnail;
    private org.springframework.web.multipart.MultipartFile detailThumbnail;

    // 파일 경로 (DB 저장용)
    private String thumbnailUrl;
    private String detailImgUrl;
}
