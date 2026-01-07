package com.learnit.learnit.admin;

import com.learnit.learnit.category.CategoryService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final AdminCourseMapper adminCourseMapper;
    private final UserMapper userMapper;

    @Value("${file.upload-dir.course:src/main/resources/static/uploads/course}")
    private String courseUploadDir;

    @Transactional
    public void createCourse(AdminCourseCreateDTO dto) {
        // 상시 오픈 체크 시 날짜 NULL 처리
        if (dto.isAlwaysOpen()) {
            dto.setStartDate(null);
            dto.setEndDate(null);
        }

        try {
            // 썸네일 저장
            if (dto.getThumbnail() != null && !dto.getThumbnail().isEmpty()) {
                String path = saveFile(dto.getThumbnail());
                dto.setThumbnailUrl(path);
            }

            // 상세 이미지(또는 PDF) 저장
            if (dto.getDetailThumbnail() != null && !dto.getDetailThumbnail().isEmpty()) {
                MultipartFile file = dto.getDetailThumbnail();
                String originalFilename = file.getOriginalFilename();
                
                if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
                    // PDF인 경우 텍스트 추출하여 .txt로 저장
                    String textPath = savePdfAsText(file);
                    dto.setDetailImgUrl(textPath);
                } else {
                    // 이미지인 경우 그대로 저장
                    String path = saveFile(file);
                    dto.setDetailImgUrl(path);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }

        adminCourseMapper.insertCourse(dto);
    }

    private String savePdfAsText(MultipartFile file) throws IOException {
        // 1. PDF 파일 임시 저장
        File tempPdf = File.createTempFile("temp", ".pdf");
        file.transferTo(tempPdf);

        // 2. 텍스트 추출
        String text;
        try (PDDocument document = Loader.loadPDF(tempPdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 시각적 위치에 따라 텍스트 정렬 (레이아웃 개선)
            text = stripper.getText(document);
        } finally {
            tempPdf.delete();
        }

        // 3. .txt 파일로 저장
        String savedFilename = UUID.randomUUID().toString() + ".txt";
        
        // 디렉토리 생성 (설정값 없으면 기본값 사용)
        String uploadDir = (courseUploadDir != null && !courseUploadDir.isEmpty()) 
                ? courseUploadDir 
                : "src/main/resources/static/uploads/course";
                
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(savedFilename);
        Files.writeString(filePath, text, StandardCharsets.UTF_8);

        return "/uploads/course/" + savedFilename;
    }

    private String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 고유 파일명 생성
        String savedFilename = UUID.randomUUID().toString() + extension;
        
        // 디렉토리 생성 (설정값 없으면 기본값 사용)
        String uploadDir = (courseUploadDir != null && !courseUploadDir.isEmpty()) 
                ? courseUploadDir 
                : "src/main/resources/static/uploads/course";
                
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 파일 저장
        Path filePath = uploadPath.resolve(savedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Web 접근 경로 반환
        return "/uploads/course/" + savedFilename;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> searchInstructors(String keyword) {
        return userMapper.searchInstructors(keyword);
    }

    @Transactional(readOnly = true)
    public List<AdminCourse> getCourses(int page, int size, String status, String search) {
        int offset = (page - 1) * size;
        return adminCourseMapper.selectCourses(offset, size, status, search);
    }

    @Transactional(readOnly = true)
    public int getCourseCount(String status, String search) {
        return adminCourseMapper.countCourses(status, search);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        adminCourseMapper.deleteCourse(courseId);
    }
}
