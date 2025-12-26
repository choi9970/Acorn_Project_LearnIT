package com.learnit.learnit.user.dto;

import lombok.Data;

@Data
public class SignupRequestDTO {
    private String email;
    private String password;
    private String passwordConfirm;
    private String name;
    private String nickname;
    private String phone;
    private String region;
}

