package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.CourseSummaryDTO;
import com.learnit.learnit.mypage.service.MyCoursesService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mypage/courses")
@RequiredArgsConstructor
public class MyCoursesController {

    private final MyCoursesService mypageCoursesService;
    private final UserService userService;

    /**
     * 내 학습 강의 페이지 조회
     */
    @GetMapping
    public String myCourses(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);
        
        // 수강 중인 강의 목록 조회
        List<CourseSummaryDTO> myCourses = mypageCoursesService.getMyCourses(userId);
        model.addAttribute("courses", myCourses);

        return "mypage/courses/courses";
    }
}
