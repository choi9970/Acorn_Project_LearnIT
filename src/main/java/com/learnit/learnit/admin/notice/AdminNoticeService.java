package com.learnit.learnit.admin.notice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNoticeService {

    private final AdminNoticeRepository repo;

    public List<AdminNoticeDto> getNotices(int page, int size, String category, String search) {
        int offset = (page - 1) * size;
        return repo.selectNotices(offset, size, category, search);
    }

    public int getTotalCount(String category, String search) {
        return repo.countNotices(category, search);
    }

    public AdminNoticeDto getNotice(int noticeId) {
        return repo.selectNoticeById(noticeId);
    }

    /**
     * ✅ AUTO_INCREMENT 미사용
     * ✅ 가장 작은 빈 notice_id 채번
     * ✅ GET_LOCK으로 동시 등록 충돌 방지
     */
    @Transactional
    public void create(AdminNoticeDto dto) {
        validate(dto);

        final String lockName = "notice_id_lock";
        int locked = repo.getNoticeIdLock(lockName);
        if (locked != 1) {
            throw new IllegalStateException("공지 등록 락 획득 실패(잠시 후 재시도)");
        }

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

    // ✅✅✅ 컨트롤러에서 호출하는 메서드 (컴파일 에러 해결)
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
}
