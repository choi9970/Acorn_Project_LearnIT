package com.learnit.learnit.course;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDTO {

    private Integer courseId;        // PK
    private Integer categoryId;      // 카테고리 FK
    private Integer userId;          // 교수자 ID

    private String title;            // 제목
    private String description;      // 상세 설명
    private Integer price;           // 가격 (0이면 무료)

    private String thumbnailUrl;     // 리스트용 썸네일
    private String detailImgUrl;     // 상세 페이지 이미지

    private String openType;         // ALWAYS / PERIOD
    private LocalDateTime openStart; // 제한 강의 시작일
    private LocalDateTime openEnd;   // 제한 강의 종료일

    private String status;           // CLOSE / ACTIVE

    private LocalDateTime createdAt; // 생성일
    private LocalDateTime updatedAt; // 수정일

    private Integer deleteFlg;       // 삭제 플래그 (0:N, 1:Y)
}