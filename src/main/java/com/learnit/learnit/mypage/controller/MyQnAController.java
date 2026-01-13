package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.MyQnADTO;
import com.learnit.learnit.mypage.service.MyQnAService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/mypage/qa")
@RequiredArgsConstructor
public class MyQnAController {

    private final MyQnAService qnAService;
    private final UserService userService;

    private static final int PAGE_BLOCK_SIZE = 5;

    /**
     * 마이페이지 강의 Q&A 목록 조회
     * 관리자(ADMIN): 모든 Q&A 조회
     * 서브 어드민(SUB_ADMIN): 관리하는 강의의 Q&A만 조회
     * 일반 사용자: 본인이 작성한 Q&A만 조회
     */
    @GetMapping
    public String qnaList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "4") int size,
            Model model, 
            HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 권한 확인
        String userRole = (String) session.getAttribute("LOGIN_USER_ROLE");
        if (userRole == null) {
            userRole = "USER"; // 기본값
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);

        // 페이징 처리 (권한에 따라 분기)
        int totalCount = qnAService.getMyQnACount(userId, userRole);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages <= 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        // Q&A 목록 조회 (권한에 따라 분기)
        List<MyQnADTO> qnaList = qnAService.getMyQnAList(userId, userRole, page, size);
        model.addAttribute("qnaList", qnaList != null ? qnaList : new java.util.ArrayList<>());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pageSize", size);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("userRole", userRole); // 권한 정보도 전달

        return "mypage/qna/myQna";
    }
}
