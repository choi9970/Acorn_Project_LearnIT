package com.learnit.learnit.courseVideo.dto;

import lombok.Data;

@Data
public class CourseVideo {
    private Long chapterId;
    private Long courseId;
    private String title;
    private Long orderIndex;
    private String videoUrl;
    private Long durationSec;
    private String sectionTitle;
}
