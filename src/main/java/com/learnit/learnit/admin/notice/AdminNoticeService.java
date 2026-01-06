package com.learnit.learnit.admin.notice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNoticeService {

    private final AdminNoticeRepository repo;

    public List<AdminNoticeDto> getNotices(int page, int size, String category, String search) {
        int offset = (page - 1) * size;
        List<AdminNoticeDto> list = repo.selectNotices(offset, size, category, search);
        list.forEach(this::enrichOriginalFileName);
        return list;
    }

    public int getTotalCount(String category, String search) {
        return repo.countNotices(category, search);
    }

    public AdminNoticeDto getNotice(int noticeId) {
        AdminNoticeDto dto = repo.selectNoticeById(noticeId);
        enrichOriginalFileName(dto);
        return dto;
    }

    @Transactional
    public void create(AdminNoticeDto dto) {
        validate(dto);
        // ✅ 테이블이 AUTO_INCREMENT라면 notice_id를 직접 안 넣어도 됨.
        // 하지만 너 코드(insertNotice)가 notice_id를 직접 넣는 구조면,
        // 기존 로직 유지해도 됨(아래는 너가 쓰던 로직 그대로).
        final String lockName = "notice_id_lock";
        int locked = repo.getNoticeIdLock(lockName);
        if (locked != 1) throw new IllegalStateException("공지 등록 락 획득 실패(잠시 후 재시도)");

        try {
            Integer newId = repo.selectSmallestMissingNoticeId();
            if (newId == null) newId = 1;

            dto.setNoticeId(newId);
            repo.insertNotice(dto);
        } finally {
            repo.releaseNoticeIdLock(lockName);
        }
    }

    @Transactional
    public void update(AdminNoticeDto dto) {
        if (dto.getNoticeId() == null) throw new IllegalArgumentException("noticeId가 없습니다.");
        validate(dto);
        repo.updateNotice(dto);
    }

    @Transactional
    public void delete(int noticeId) {
        repo.deleteNotice(noticeId);
    }

    @Transactional
    public void deleteSelected(List<Integer> noticeIds) {
        repo.deleteNoticesByIds(noticeIds);
    }

    @Transactional
    public void deleteAllByFilter(String category, String search) {
        repo.deleteAllByFilter(category, search);
    }

    private void validate(AdminNoticeDto dto) {
        if (dto.getCategory() == null || dto.getCategory().isBlank())
            throw new IllegalArgumentException("카테고리를 선택하세요.");
        if (dto.getTitle() == null || dto.getTitle().isBlank())
            throw new IllegalArgumentException("제목을 입력하세요.");
        if (dto.getContent() == null || dto.getContent().isBlank())
            throw new IllegalArgumentException("내용을 입력하세요.");
    }

    private void enrichOriginalFileName(AdminNoticeDto dto) {
        if (dto == null) return;
        if (dto.getFileUrl() == null || dto.getFileUrl().isBlank()) return;

        String stored = Paths.get(dto.getFileUrl()).getFileName().toString(); // uuid__원본.txt
        int idx = stored.indexOf("__");
        dto.setOriginalFileName(idx >= 0 ? stored.substring(idx + 2) : stored);
    }
}
