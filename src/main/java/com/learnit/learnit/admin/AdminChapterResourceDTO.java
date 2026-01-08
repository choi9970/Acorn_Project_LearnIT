package com.learnit.learnit.admin;

import lombok.Data;

@Data
public class AdminChapterResourceDTO {
    private Long resourceId;
    private String originalFilename;
    private String fileUrl;
    private String fileType;
}
