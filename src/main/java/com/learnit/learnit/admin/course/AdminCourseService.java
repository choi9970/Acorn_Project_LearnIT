package com.learnit.learnit.admin.course;

import com.learnit.learnit.admin.AdminChapterDTO;
import com.learnit.learnit.admin.AdminChapterInsertDTO;
import com.learnit.learnit.admin.AdminChapterResourceDTO;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final AdminCourseMapper adminCourseMapper;
    private final UserMapper userMapper;

    @Value("${file.upload-dir.course:uploads/course}")
    private String courseUploadDir;

    /* --- 조회 로직 --- */

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

    @Transactional(readOnly = true)
    public AdminCourseCreateDTO getCourseDetail(Long courseId) {
        AdminCourseDetailDTO detail = adminCourseMapper.selectCourseById(courseId);
        if (detail == null) {
            throw new IllegalArgumentException("존재하지 않는 강의입니다.");
        }

        AdminCourseCreateDTO dto = mapToCreateDTO(detail);
        
        // 커리큘럼 매핑
        List<AdminChapterDTO> chapters = adminCourseMapper.selectChaptersWithResources(courseId);
        dto.setSections(mapChaptersToSections(chapters));

        return dto;
    }

    /* --- 등록/수정/삭제 로직 --- */

    @Transactional
    public void createCourse(AdminCourseCreateDTO dto) {
        if (dto.isAlwaysOpen()) {
            dto.setStartDate(null);
            dto.setEndDate(null);
        }

        try {
            handleThumbnailUploads(dto);
        } catch (IOException e) {
            throw new RuntimeException("썸네일 저장 중 오류가 발생했습니다.", e);
        }

        adminCourseMapper.insertCourse(dto);
        
        // 신규 생성 시에는 단순 저장
        saveNewCurriculum(dto);
    }

    @Transactional
    public void updateCourse(Long courseId, AdminCourseCreateDTO dto) {
        dto.setCourseId(courseId);
        if (dto.isAlwaysOpen()) {
            dto.setStartDate(null);
            dto.setEndDate(null);
        }

        try {
            // 새 파일이 있는 경우에만 업데이트 (DTO 내부 필드 갱신)
            handleThumbnailUploads(dto);
        } catch (IOException e) {
            throw new RuntimeException("썸네일 저장 중 오류가 발생했습니다.", e);
        }

        adminCourseMapper.updateCourse(dto);

        // 커리큘럼 스마트 업데이트 (Diff & Sync)
        syncCurriculum(courseId, dto);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        adminCourseMapper.deleteCourse(courseId);
    }

    /* --- Helper Methods: Mapping --- */

    private AdminCourseCreateDTO mapToCreateDTO(AdminCourseDetailDTO detail) {
        AdminCourseCreateDTO dto = new AdminCourseCreateDTO();
        dto.setCourseId(detail.getCourseId());
        dto.setTitle(detail.getTitle());
        dto.setDescription(detail.getDescription());
        dto.setCategoryId(detail.getCategoryId());
        dto.setPrice(detail.getPrice());

        if (detail.getInstructorId() != null) {
            dto.setInstructorIds(Collections.singletonList(detail.getInstructorId()));
            dto.setInstructorName(detail.getInstructorName());
            dto.setInstructorNickname(detail.getInstructorNickname());
        }

        dto.setStartDate(detail.getStartDate());
        dto.setEndDate(detail.getEndDate());
        dto.setAlwaysOpen(detail.getStartDate() == null && detail.getEndDate() == null);

        dto.setThumbnailUrl(detail.getThumbnailUrl());
        dto.setDetailImgUrl(detail.getDetailImgUrl());
        
        // 파일명 추출 (URL에서 마지막 부분)
        dto.setThumbnailFileName(extractFileName(detail.getThumbnailUrl()));
        dto.setDetailThumbnailFileName(extractFileName(detail.getDetailImgUrl()));

        return dto;
    }

    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) return null;
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private List<AdminCourseCreateDTO.SectionRequest> mapChaptersToSections(List<AdminChapterDTO> chapters) {
        // 섹션별 그룹화 (LinkedHashMap으로 순서 유지)
        Map<String, List<AdminChapterDTO>> sectionsMap = chapters.stream()
                .collect(Collectors.groupingBy(AdminChapterDTO::getSectionTitle, LinkedHashMap::new, Collectors.toList()));

        List<AdminCourseCreateDTO.SectionRequest> sectionRequests = new ArrayList<>();
        
        for (Map.Entry<String, List<AdminChapterDTO>> entry : sectionsMap.entrySet()) {
            AdminCourseCreateDTO.SectionRequest sectionReq = new AdminCourseCreateDTO.SectionRequest();
            sectionReq.setTitle(entry.getKey());
            
            List<AdminCourseCreateDTO.ChapterRequest> chapterReqs = new ArrayList<>();
            for (AdminChapterDTO chapter : entry.getValue()) {
                AdminCourseCreateDTO.ChapterRequest chapterReq = new AdminCourseCreateDTO.ChapterRequest();
                chapterReq.setChapterId(chapter.getChapterId());
                chapterReq.setTitle(chapter.getTitle());
                chapterReq.setVideoUrl(chapter.getVideoUrl());
                
                if (chapter.getResources() != null && !chapter.getResources().isEmpty()) {
                    AdminChapterResourceDTO res = chapter.getResources().get(0);
                    chapterReq.setExistingFileUrl(res.getFileUrl());
                    chapterReq.setExistingFileName(res.getOriginalFilename());
                }
                chapterReqs.add(chapterReq);
            }
            sectionReq.setChapters(chapterReqs);
            sectionRequests.add(sectionReq);
        }
        return sectionRequests;
    }

    /* --- Helper Methods: Curriculum Logic --- */

    private void saveNewCurriculum(AdminCourseCreateDTO dto) {
        if (dto.getSections() == null) return;

        Long courseId = dto.getCourseId();
        int totalOrderIndex = 1;

        for (AdminCourseCreateDTO.SectionRequest section : dto.getSections()) {
            if (section == null || section.getChapters() == null) continue;

            for (AdminCourseCreateDTO.ChapterRequest chapterReq : section.getChapters()) {
                if (isInvalidChapter(chapterReq)) continue;
                insertChapter(courseId, section.getTitle(), chapterReq, totalOrderIndex++);
            }
        }
    }

    private void syncCurriculum(Long courseId, AdminCourseCreateDTO dto) {
        // 1. 기존 데이터 조회
        List<AdminChapterDTO> existingChapters = adminCourseMapper.selectChaptersWithResources(courseId);
        Set<Long> processedChapterIds = new HashSet<>();
        int totalOrderIndex = 1;

        // 2. 요청 데이터 처리 (Update & Insert)
        if (dto.getSections() != null) {
            for (AdminCourseCreateDTO.SectionRequest section : dto.getSections()) {
                if (section == null || section.getChapters() == null) continue;

                for (AdminCourseCreateDTO.ChapterRequest chapterReq : section.getChapters()) {
                    if (isInvalidChapter(chapterReq)) continue;

                    if (chapterReq.getChapterId() == null) {
                        // Insert
                        insertChapter(courseId, section.getTitle(), chapterReq, totalOrderIndex++);
                    } else {
                        // Update
                        processedChapterIds.add(chapterReq.getChapterId());
                        updateChapter(section.getTitle(), chapterReq, totalOrderIndex++, existingChapters);
                    }
                }
            }
        }

        // 3. 삭제 처리 (Delete Orphaned Chapters)
        deleteOrphanedChapters(existingChapters, processedChapterIds);
    }

    private boolean isInvalidChapter(AdminCourseCreateDTO.ChapterRequest req) {
        return req == null || (req.getTitle() == null && req.getVideoUrl() == null);
    }

    private void insertChapter(Long courseId, String sectionTitle, AdminCourseCreateDTO.ChapterRequest req, int orderIndex) {
        AdminChapterInsertDTO newChapter = new AdminChapterInsertDTO();
        newChapter.setCourseId(courseId);
        newChapter.setSectionTitle(sectionTitle);
        newChapter.setTitle(req.getTitle());
        newChapter.setOrderIndex(orderIndex);
        newChapter.setVideoUrl(req.getVideoUrl());

        adminCourseMapper.insertChapter(newChapter);
        
        // 새 파일이 있는 경우에만 저장
        if (req.getFile() != null && !req.getFile().isEmpty()) {
            saveChapterResource(newChapter.getChapterId(), req.getFile());
        }
    }

    private void updateChapter(String sectionTitle, AdminCourseCreateDTO.ChapterRequest req, int orderIndex, List<AdminChapterDTO> existingChapters) {
        AdminChapterInsertDTO updateDto = new AdminChapterInsertDTO();
        updateDto.setChapterId(req.getChapterId());
        updateDto.setSectionTitle(sectionTitle);
        updateDto.setTitle(req.getTitle());
        updateDto.setOrderIndex(orderIndex);
        updateDto.setVideoUrl(req.getVideoUrl());

        adminCourseMapper.updateChapter(updateDto);

        // 리소스 동기화
        syncChapterResource(req.getChapterId(), req, existingChapters);
    }

    private void syncChapterResource(Long chapterId, AdminCourseCreateDTO.ChapterRequest req, List<AdminChapterDTO> existingChapters) {
        // 기존 리소스 찾기
        AdminChapterResourceDTO oldResource = existingChapters.stream()
                .filter(ch -> ch.getChapterId().equals(chapterId))
                .findFirst()
                .map(AdminChapterDTO::getResources)
                .filter(list -> list != null && !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);

        // Case 1: 새 파일 업로드 -> 교체
        if (req.getFile() != null && !req.getFile().isEmpty()) {
            if (oldResource != null) adminCourseMapper.deleteChapterResource(oldResource.getResourceId());
            saveChapterResource(chapterId, req.getFile());
        }
        // Case 2: 파일 삭제 요청 (기존엔 있었는데 hidden url이 비어옴) -> 삭제
        else if (oldResource != null && (req.getExistingFileUrl() == null || req.getExistingFileUrl().isEmpty())) {
            adminCourseMapper.deleteChapterResource(oldResource.getResourceId());
        }
        // Case 3: 유지 (아무 작업 안 함)
    }

    private void deleteOrphanedChapters(List<AdminChapterDTO> existingChapters, Set<Long> processedIds) {
        for (AdminChapterDTO oldChapter : existingChapters) {
            if (!processedIds.contains(oldChapter.getChapterId())) {
                adminCourseMapper.deleteChapterResourcesByChapterId(oldChapter.getChapterId());
                adminCourseMapper.deleteChapter(oldChapter.getChapterId());
            }
        }
    }

    /* --- Helper Methods: File Handling --- */

    private void handleThumbnailUploads(AdminCourseCreateDTO dto) throws IOException {
        // 메인 썸네일
        if (dto.getThumbnail() != null && !dto.getThumbnail().isEmpty()) {
            dto.setThumbnailUrl(saveFile(dto.getThumbnail()));
        } else if (dto.getThumbnailUrl() == null) {
            // 유지되는 경우 DTO에 기존 URL이 있어야 함 (Service 호출 전 바인딩 확인 필요, 
            // 현재 구조에선 updateCourse 호출 시 dto에 기존 url이 없으면 null이 되므로,
            // Controller에서 병합하거나, 여기서 null이면 기존 값 유지가 안됨.
            // 하지만 form에서 hidden으로 url을 보내주지 않으므로 DB 조회가 안전.)
            if (dto.getCourseId() != null) {
                AdminCourseDetailDTO existing = adminCourseMapper.selectCourseById(dto.getCourseId());
                dto.setThumbnailUrl(existing.getThumbnailUrl());
            }
        }

        // 상세 썸네일 (이미지 or PDF)
        if (dto.getDetailThumbnail() != null && !dto.getDetailThumbnail().isEmpty()) {
            MultipartFile file = dto.getDetailThumbnail();
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
                dto.setDetailImgUrl(savePdfAsText(file));
            } else {
                dto.setDetailImgUrl(saveFile(file));
            }
        } else if (dto.getDetailImgUrl() == null && dto.getCourseId() != null) {
            AdminCourseDetailDTO existing = adminCourseMapper.selectCourseById(dto.getCourseId());
            dto.setDetailImgUrl(existing.getDetailImgUrl());
        }
    }

    private void saveChapterResource(Long chapterId, MultipartFile file) {
        try {
            String fileUrl = saveFile(file);
            String originalFilename = file.getOriginalFilename();
            String fileType = extractExtension(originalFilename);
            adminCourseMapper.insertChapterResource(chapterId, originalFilename, fileUrl, fileType);
        } catch (IOException e) {
            throw new RuntimeException("챕터 자료 파일 저장 실패", e);
        }
    }

    private String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) return null;
        String savedFilename = UUID.randomUUID() + "." + extractExtension(file.getOriginalFilename());
        Path filePath = getUploadPath().resolve(savedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/course/" + savedFilename;
    }

    private String savePdfAsText(MultipartFile file) throws IOException {
        File tempPdf = File.createTempFile("temp", ".pdf");
        file.transferTo(tempPdf);
        try (PDDocument document = Loader.loadPDF(tempPdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            
            String savedFilename = UUID.randomUUID() + ".txt";
            Path filePath = getUploadPath().resolve(savedFilename);
            Files.writeString(filePath, text, StandardCharsets.UTF_8);
            return "/uploads/course/" + savedFilename;
        } finally {
            tempPdf.delete();
        }
    }

    private Path getUploadPath() throws IOException {
        Path uploadPath = Paths.get(courseUploadDir).isAbsolute() ? 
                Paths.get(courseUploadDir) : Paths.get(System.getProperty("user.dir"), courseUploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        return uploadPath;
    }

    private String extractExtension(String filename) {
        return (filename != null && filename.contains(".")) ? 
                filename.substring(filename.lastIndexOf(".") + 1) : "";
    }
}
