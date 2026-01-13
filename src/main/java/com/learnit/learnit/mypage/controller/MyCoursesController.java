package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.MyCourseSummaryDTO;
import com.learnit.learnit.mypage.service.MyCoursesService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/mypage/courses")
@RequiredArgsConstructor
public class MyCoursesController {

    private final MyCoursesService mypageCoursesService;
    private final UserService userService;

    private static final int PAGE_BLOCK_SIZE = 5;

    /**
     * 내 학습 강의 페이지 조회 (페이징)
     * 관리자(ADMIN): 모든 학습 강의 조회
     * 서브 어드민(SUB_ADMIN): 관리하는 강의의 학습 강의만 조회
     * 일반 사용자: 본인의 학습 강의만 조회
     */
    @GetMapping
    public String myCourses(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "4") int size,
            Model model,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

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

        // 전체 강의 개수 조회 (권한에 따라 분기)
        int totalCount = mypageCoursesService.getMyCoursesCount(userId, userRole);
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages <= 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startPage = ((page - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, totalPages);

        // 학습 강의 목록 조회 (페이징, 권한에 따라 분기)
        List<MyCourseSummaryDTO> myCourses = mypageCoursesService.getMyCourses(userId, userRole, page, size);
        model.addAttribute("courses", myCourses != null ? myCourses : new java.util.ArrayList<>());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pageSize", size);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "mypage/courses/myCourses";
    }
}
