package com.learnit.learnit.mypage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    @Value("${file.upload-dir:src/main/resources/static/uploads/profile}")
    private String uploadDir;

    /**
     * 프로필 이미지 업로드
     */
    public String uploadProfileImage(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        String extension = getFileExtension(originalFilename);
        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (JPG, PNG, GIF만 가능)");
        }

        // 파일 크기 검증 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 5MB 이하여야 합니다.");
        }

        // 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 파일명 생성 (userId_timestamp_uuid.extension)
        String filename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        // 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 상대 경로 반환 (웹에서 접근 가능한 경로)
        return "/uploads/profile/" + filename;
    }

    /**
     * 프로필 이미지 삭제
     */
    public boolean deleteProfileImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || imageUrl.equals("/images/logo_icon.png")) {
            return false;
        }

        try {
            // /uploads/profile/filename 형식에서 filename 추출
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadDir).resolve(filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("프로필 이미지 삭제 완료: {}", filename);
                return true;
            }
        } catch (IOException e) {
            log.error("프로필 이미지 삭제 실패: {}", imageUrl, e);
        }

        return false;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }

    /**
     * 이미지 확장자 검증
     */
    private boolean isValidImageExtension(String extension) {
        return extension.equals(".jpg") || extension.equals(".jpeg") || 
               extension.equals(".png") || extension.equals(".gif");
    }
}

