package com.learnit.learnit.admin.notice;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminNoticeDto {
    private Integer noticeId;
    private Integer userId;

    private String category;   // '긴급'/'일반'/'이벤트'
    private String title;
    private String content;
    private String fileUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 조인용(표시)
    private String writerName;

    // ✅ 화면 표시용 (DB컬럼 아님): fileUrl에서 원본 파일명 추출
    private String originalFileName;
}
