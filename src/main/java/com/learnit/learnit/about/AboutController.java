package com.learnit.learnit.about;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutController {

    @Value("${kakao.maps.js-key}")
    private String kakaoMapKey;

    @GetMapping("/about")
    public String about(Model model) {

        // ✅ GitHub 외부 API에서 불러올 계정(조직/유저)명
        // 본인 조직명으로 바꿔줘 (예: learnit-team, acorn-learnit 등)
        model.addAttribute("githubUser", "stopcircle");

        // ✅ 카카오 지도 API 키
        model.addAttribute("kakaoMapKey", kakaoMapKey);

        // ✅ 회사 위치 (여기만 실제 값으로 바꿔줘)
        model.addAttribute("officeName", "LearnIT 본사");
        model.addAttribute("officeAddress", "서울특별시 마포구 양화로 122 LAB7 빌딩 3층, 4층");
        model.addAttribute("officeLat", 37.5569);   // 위도
        model.addAttribute("officeLng", 126.9234);  // 경도

        return "about/about";
    }
}
