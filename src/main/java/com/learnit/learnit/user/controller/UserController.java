package com.learnit.learnit.user.controller;

import com.learnit.learnit.user.dto.LoginRequestDTO;
import com.learnit.learnit.user.dto.SignupRequestDTO;
import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.mapper.UserMapper;
import com.learnit.learnit.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "로그인에 실패했습니다.");
        }
        return "user/login";
    }

    @PostMapping("/login")
    public String doLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        User user = userService.login(request);

        if (user == null) {
            model.addAttribute("error", "이메일 또는 비밀번호가 맞지 않습니다.");
            return "user/login";
        }

        userService.setLoginSession(session, user);
        return "redirect:/home";
    }

    @GetMapping("/signup")
    public String showSignupForm() {
        return "user/signup";
    }

    @PostMapping("/signup")
    public String doSignup(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String passwordConfirm,
            @RequestParam String name,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String region,
            Model model
    ) {
        SignupRequestDTO request = new SignupRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        request.setPasswordConfirm(passwordConfirm);
        request.setName(name);
        request.setNickname(nickname);
        request.setPhone(phone);
        request.setRegion(region);

        // 비밀번호 확인 검증
        if (passwordConfirm != null && !password.equals(passwordConfirm)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "user/signup";
        }

        try {
            userService.signup(request);
            return "redirect:/login?success=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "user/signup";
        }
    }

    @GetMapping("/user/additional-info")
    public String showAdditionalInfoForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return "redirect:/login";
        }
        
        User user = userMapper.selectUserEntityById(userId);
        if (user == null) {
            return "redirect:/login";
        }
        
        // ACTIVE 상태면 이미 가입 완료된 사용자이므로 홈으로 리다이렉트
        if (User.STATUS_ACTIVE.equals(user.getStatus())) {
            return "redirect:/home";
        }
        
        // SIGNUP_PENDING 상태만 추가 정보 입력 페이지 표시
        return "user/additional-info";
    }

    @PostMapping("/user/additional-info")
    public String submitAdditionalInfo(
            @RequestParam String phone,
            @RequestParam String region,
            HttpSession session,
            Model model
    ) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return "redirect:/login";
        }
        
        User user = userMapper.selectUserEntityById(userId);
        if (user == null) {
            return "redirect:/login";
        }
        
        // 유효성 검사
        if (phone == null || phone.trim().isEmpty()) {
            model.addAttribute("error", "전화번호를 입력해주세요.");
            return "user/additional-info";
        }
        
        if (region == null || region.trim().isEmpty()) {
            model.addAttribute("error", "활동 지역을 선택해주세요.");
            return "user/additional-info";
        }
        
        // 추가 정보 업데이트 및 상태 변경
        user.setPhone(phone.trim());
        user.setRegion(region.trim());
        user.setStatus(User.STATUS_ACTIVE); // 추가 정보 입력 완료 → ACTIVE
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateUser(user);
        
        return "redirect:/home";
    }

    @PostMapping("/logout")
    public String doLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/home";
    }
}

