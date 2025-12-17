package com.learnit.learnit.notice;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private static final int PAGE_SIZE = 5; // ✅ 한 페이지 5개

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public List<Notice> getNotices(int page) {
        int safePage = Math.max(page, 1);
        int offset = (safePage - 1) * PAGE_SIZE;
        return noticeMapper.findPage(PAGE_SIZE, offset);
    }

    public int getTotalPages() {
        int total = noticeMapper.countAll();
        return (int) Math.ceil((double) total / PAGE_SIZE);
    }

    public Notice getNotice(Long id) {
        return noticeMapper.findById(id);
    }
}
