package com.learnit.learnit.mypage;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyPageController {

    @GetMapping("/mypage")
    public String myPage(Model model) {

        model.addAttribute("recentLecture", "스프링부트 입문");
        model.addAttribute("weeklyStudy", "총 5시간");
        model.addAttribute("certificateCount", 3);

        return "mypage/main";
    }
}
