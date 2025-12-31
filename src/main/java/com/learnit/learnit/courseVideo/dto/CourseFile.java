package com.learnit.learnit.courseVideo.dto;

import lombok.Data;

@Data
public class CourseFile {
    private Long fileId;
    private Long chapterId;
    private String title;
    private String fileUrl;
    private String fileType;
}
