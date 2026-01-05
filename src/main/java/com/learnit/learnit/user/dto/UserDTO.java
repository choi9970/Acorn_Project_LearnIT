package com.learnit.learnit.user.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long userId;
    private String nickname;
    private String email;
    private String profileImageUrl;

    //관리자 기능을 위해 추가 사용 필드
    private String name;         //실명 검색용
    private String phoneNumber;  //전화번호
    private String status;       //유저 상태 체크용
}

