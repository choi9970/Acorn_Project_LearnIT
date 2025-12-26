package com.learnit.learnit.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    // 사용자 상태 상수
    public static final String STATUS_SIGNUP_PENDING = "SIGNUP_PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BANNED = "BANNED";
    public static final String STATUS_DELETE = "DELETE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String region;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(length = 20, nullable = false)
    private String role;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(length = 20)
    private String provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "profile_img", length = 500)
    private String profileImg;

    @Column(name = "email_verified", length = 1)
    private String emailVerified;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 추가 정보 입력 완료 여부 확인
     * 전화번호와 지역 정보가 모두 입력되었는지 확인
     */
    public boolean isAdditionalInfoCompleted() {
        return phone != null && !phone.isEmpty() 
            && region != null && !region.isEmpty();
    }
}

