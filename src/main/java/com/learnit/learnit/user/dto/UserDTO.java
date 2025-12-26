package com.learnit.learnit.user.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long userId;
    private String nickname;
    private String email;
    private String profileImageUrl;
}

