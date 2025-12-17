package com.learnit.learnit.home;

import lombok.Data;

@Data
public class MainCourse {

    //메인 페이지 -> 인기강좌 리스트 출력용 DTO
    private Long courseId;
    private String categoryName;
    private String title;
    private Integer price;
    private String thumbnailUrl;
    private String status;        // 모집중 표시
    private Integer salesCount;   //판매순위
}
