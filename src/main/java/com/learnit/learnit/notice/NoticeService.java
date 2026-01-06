package com.learnit.learnit.notice;

import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private static final int PAGE_SIZE = 5;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public List<Notice> getNotices(int page) {
        int safePage = Math.max(page, 1);
        int offset = (safePage - 1) * PAGE_SIZE;

        List<Notice> list = noticeMapper.findPage(PAGE_SIZE, offset);
        list.forEach(this::enrichOriginalFileName);
        return list;
    }

    public int getTotalPages() {
        int total = noticeMapper.countAll();
        return (int) Math.ceil((double) total / PAGE_SIZE);
    }

    public Notice getNotice(Long id) {
        Notice n = noticeMapper.findById(id);
        enrichOriginalFileName(n);
        return n;
    }

    // ✅ fileUrl이 "/uploads/notice/uuid__원본.ext" 형태면 원본만 추출
    private void enrichOriginalFileName(Notice notice) {
        if (notice == null) return;
        if (notice.getFileUrl() == null || notice.getFileUrl().isBlank()) return;

        String stored = Paths.get(notice.getFileUrl()).getFileName().toString(); // uuid__원본
        int idx = stored.indexOf("__");
        notice.setOriginalFileName(idx >= 0 ? stored.substring(idx + 2) : stored);
    }
}
