package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.CertificateDTO;
import com.learnit.learnit.mypage.dto.ProfileDTO;
import com.learnit.learnit.mypage.dto.ProfileUpdateDTO;
import com.learnit.learnit.mypage.mapper.ProfileMapper;
import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.repository.UserRepository;
import com.learnit.learnit.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final UserRepository userRepository;
    private final EmailService emailService;

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
     * 사용자 수료증 목록 조회
     */
    public List<CertificateDTO> getCertificates(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }
        return profileMapper.selectCertificatesByUserId(userId);
    }

    /**
     * 개인정보 수정
     */
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateDTO updateDTO, String currentPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        // 비밀번호 변경 시 현재 비밀번호 검증
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                throw new IllegalArgumentException("비밀번호를 변경하려면 현재 비밀번호를 입력해주세요.");
            }
            
            // 사용자 정보 조회
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            
            // 소셜 로그인 사용자는 비밀번호가 없을 수 있음
            if (user.getProvider() != null && !user.getProvider().isEmpty() && !user.getProvider().equals("local")) {
                throw new IllegalArgumentException("소셜 로그인으로 가입한 계정은 비밀번호를 변경할 수 없습니다.");
            }
            
            // 현재 비밀번호 검증
            if (user.getPassword() == null || !passwordEncoder.matches(currentPassword.trim(), user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            
            // 새 비밀번호 유효성 검증 (최소 8자 이상)
            String password = updateDTO.getPassword().trim();
            if (password.length() < 8) {
                throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
            }
        }

        // 비밀번호 암호화 처리 및 이메일 발송
        String encodedPassword = null;
        boolean passwordChanged = false;
        String userEmail = null;
        String userName = null;
        
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
            encodedPassword = passwordEncoder.encode(updateDTO.getPassword().trim());
            passwordChanged = true;
            
            // 사용자 정보 조회 (이메일 발송용)
            User user = userRepository.findById(userId)
                .orElse(null);
            if (user != null) {
                userEmail = user.getEmail();
                userName = user.getName();
            }
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
        
        // 비밀번호 변경 시 이메일 발송
        if (passwordChanged && userEmail != null) {
            try {
                emailService.sendPasswordChangedEmail(userEmail, userName);
            } catch (Exception e) {
                // 이메일 발송 실패는 로깅만 하고 예외를 던지지 않음
                System.err.println("비밀번호 변경 알림 이메일 발송 실패: " + e.getMessage());
            }
        }
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

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdrawUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 사용자 상태를 DELETE로 변경
        user.setStatus(User.STATUS_DELETE);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        userRepository.save(user);
    }
}

