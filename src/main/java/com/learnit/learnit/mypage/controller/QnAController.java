package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.QnADTO;
import com.learnit.learnit.mypage.service.MyPageQnAService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mypage/qa")
@RequiredArgsConstructor
public class QnAController {

    private final MyPageQnAService qnAService;
    private final UserService userService;

    /**
     * 마이페이지 강의 Q&A 목록 조회
     */
    @GetMapping
    public String qnaList(Model model, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);

        // 사용자가 작성한 Q&A 목록 조회
        List<QnADTO> qnaList = qnAService.getMyQnAList(userId);
        model.addAttribute("qnaList", qnaList != null ? qnaList : new java.util.ArrayList<>());

        return "mypage/qa/qna";
    }
}

