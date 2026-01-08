package com.learnit.learnit.admin;

import lombok.Data;
import java.util.List;

@Data
public class AdminChapterDTO {
    private Long chapterId;
    private Long courseId;
    private String sectionTitle;
    private String title;
    private int orderIndex;
    private String videoUrl;
    private List<AdminChapterResourceDTO> resources;
}
