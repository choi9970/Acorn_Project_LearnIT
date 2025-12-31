package com.learnit.learnit.mypage.dto;

import lombok.Data;

@Data
public class ProfileDTO {
    private Long userId;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private String region;
    private String githubUrl;
    private String profileImageUrl;
}

