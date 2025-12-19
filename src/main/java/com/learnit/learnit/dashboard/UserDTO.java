package com.learnit.learnit.dashboard;

import lombok.Data;

@Data
public class UserDTO {
    private Long userId;
    private String nickname;
    private String email;
    private String profileImageUrl;
}

