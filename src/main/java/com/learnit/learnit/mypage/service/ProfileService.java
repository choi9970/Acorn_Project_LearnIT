package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.ProfileDTO;
import com.learnit.learnit.mypage.dto.ProfileUpdateDTO;
import com.learnit.learnit.mypage.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;

    /**
     * 사용자 프로필 조회
     */
    public ProfileDTO getProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }
        return profileMapper.selectProfileByUserId(userId);
    }

    /**
     * 개인정보 수정
     */
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateDTO updateDTO) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        // 비밀번호 암호화 처리
        String encodedPassword = null;
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
            // 비밀번호 유효성 검증 (최소 8자 이상)
            String password = updateDTO.getPassword().trim();
            if (password.length() < 8) {
                throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
            }
            encodedPassword = passwordEncoder.encode(password);
        }

        // 이메일 유효성 검증
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().trim().isEmpty()) {
            String email = updateDTO.getEmail().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
            }
        }

        // 전화번호 유효성 검증
        if (updateDTO.getPhone() != null && !updateDTO.getPhone().trim().isEmpty()) {
            String phone = updateDTO.getPhone().trim();
            if (!phone.matches("^[0-9-]+$")) {
                throw new IllegalArgumentException("올바른 전화번호 형식이 아닙니다.");
            }
        }

        // 깃허브 URL 처리 (사용자명만 입력받아서 URL로 변환)
        if (updateDTO.getGithubUrl() != null && !updateDTO.getGithubUrl().trim().isEmpty()) {
            String githubUrl = updateDTO.getGithubUrl().trim();
            // 이미 URL 형식이 아니면 사용자명으로 간주하고 URL 생성
            if (!githubUrl.startsWith("http://") && !githubUrl.startsWith("https://")) {
                if (!githubUrl.startsWith("github.com/")) {
                    githubUrl = "https://github.com/" + githubUrl;
                } else {
                    githubUrl = "https://" + githubUrl;
                }
            }
            updateDTO.setGithubUrl(githubUrl);
        }

        // 프로필 업데이트
        profileMapper.updateProfile(
            userId,
            updateDTO.getName() != null ? updateDTO.getName().trim() : null,
            encodedPassword,
            updateDTO.getEmail() != null ? updateDTO.getEmail().trim() : null,
            updateDTO.getPhone() != null ? updateDTO.getPhone().trim() : null,
            updateDTO.getGithubUrl() != null ? updateDTO.getGithubUrl().trim() : null,
            updateDTO.getRegion() != null ? updateDTO.getRegion().trim() : null,
            null // profileImg는 별도 메서드로 처리
        );
    }

    /**
     * 프로필 이미지 업데이트
     */
    @Transactional
    public String updateProfileImage(Long userId, org.springframework.web.multipart.MultipartFile file) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        try {
            // 기존 프로필 이미지 조회
            ProfileDTO profile = profileMapper.selectProfileByUserId(userId);
            String oldImageUrl = profile != null ? profile.getProfileImageUrl() : null;

            // 새 이미지 업로드
            String newImageUrl = fileUploadService.uploadProfileImage(file, userId);

            // DB 업데이트
            profileMapper.updateProfileImage(userId, newImageUrl);

            // 기존 이미지 삭제 (기본 이미지가 아닌 경우)
            if (oldImageUrl != null && !oldImageUrl.equals("/images/logo_icon.png")) {
                fileUploadService.deleteProfileImage(oldImageUrl);
            }

            return newImageUrl;
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 프로필 이미지 제거
     */
    @Transactional
    public void removeProfileImage(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        // 기존 프로필 이미지 조회
        ProfileDTO profile = profileMapper.selectProfileByUserId(userId);
        String oldImageUrl = profile != null ? profile.getProfileImageUrl() : null;

        // DB에서 이미지 제거 (기본 이미지로 설정)
        profileMapper.updateProfileImage(userId, null);

        // 기존 이미지 파일 삭제 (기본 이미지가 아닌 경우)
        if (oldImageUrl != null && !oldImageUrl.equals("/images/logo_icon.png")) {
            fileUploadService.deleteProfileImage(oldImageUrl);
        }
    }
}

