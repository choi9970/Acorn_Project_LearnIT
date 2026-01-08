package com.learnit.learnit.admin.course;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AdminCourseDetailDTO {
    private Long courseId;
    private String title;
    private String description;
    private Long categoryId;
    private int price;
    private Long instructorId;
    private String instructorName;
    private String instructorNickname;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String thumbnailUrl;
    private String detailImgUrl;
}
