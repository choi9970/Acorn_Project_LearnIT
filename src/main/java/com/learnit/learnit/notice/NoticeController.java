package com.learnit.learnit.notice;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NoticeController {

    // 목록
    @GetMapping("/notice")
    public String noticeList() {
        return "notice/noticeList";
    }

    // 상세
    @GetMapping("/notice/{id}")
    public String noticeDetail(@PathVariable("id") Long id, Model model) {

        // ✅ 지금은 DB 없이 더미 데이터(나중에 DB 붙이면 서비스/mapper로 조회해서 넣으면 됨)
        model.addAttribute("noticeId", id);
        model.addAttribute("noticeTitle", "공지사항 글제목(" + id + ")");
        model.addAttribute("noticeContent", "공지사항 글내용 영역입니다.\n여기에 내용이 들어갑니다.");

        return "notice/noticeDetail";
    }
}