package com.learnit.learnit.courseDetail;

import lombok.Data;

@Data
public class ChapterDTO {
    private int chapterId;
    private int courseId;
    private String title;
    private int orderIndex;

    // ✅ 섹션명 (DB: chapter.section_title)
    private String sectionTitle;
}
