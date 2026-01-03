package com.learnit.learnit.courseDetail;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChapterDTO {

    private Long chapterId;       // PK
    private Integer courseId;        // FK

    private String title;
    private Integer orderIndex;

    private String videoUrl;
    private Integer durationSec;

    private String sectionTitle;     // chapter.section_title (ALTER로 추가한 컬럼)

    private LocalDateTime createdAt;
}
