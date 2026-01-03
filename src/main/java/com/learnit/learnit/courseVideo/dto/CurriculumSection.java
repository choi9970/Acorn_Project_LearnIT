package com.learnit.learnit.courseVideo.dto;

import lombok.Data;

import java.util.List;

@Data
public class CurriculumSection {
    private String sectionTitle;
    private List<CourseVideo> chapters;

    public CurriculumSection(String sectionTitle, List<CourseVideo> chapters) {
        this.sectionTitle = sectionTitle;
        this.chapters = chapters;
    }
}
