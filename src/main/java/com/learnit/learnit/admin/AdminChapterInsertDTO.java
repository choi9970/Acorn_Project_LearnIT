package com.learnit.learnit.admin;

import lombok.Data;

@Data
public class AdminChapterInsertDTO {
    private Long chapterId;
    private Long courseId;
    private String sectionTitle;
    private String title;
    private int orderIndex;
    private String videoUrl;
}
