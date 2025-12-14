package com.learnit.learnit.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminHome {
    private int activeCourseCount;
    private int pendingReviewCount;
    private int pendingQnaCount;

    private List<AdminNotice> latestNotices;
    private List<AdminCertificate> pendingCertificates;

    @Data
    public static  class AdminNotice{
        private String category;
        private String title;
        private String content;
        private String createdAt;
    }

    @Data
    public static class AdminCertificate{
        private String title;
        private String name;
    }
}
