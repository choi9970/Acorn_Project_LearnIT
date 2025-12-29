package com.learnit.learnit.user.dto;

import lombok.Data;

@Data
public class OAuthUserDTO {
    private String provider;
    private String providerId;
    private String email;
    private String name;
    private String profileImg;
}

