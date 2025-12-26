package com.learnit.learnit.auth;

import com.learnit.learnit.user.User;
import com.learnit.learnit.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login/LoginTemp";
    }

    @PostMapping("/login")
    public String doLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        User user = userRepository.findByEmailAndPassword(email, password)
                .orElse(null);

        if (user == null) {
            model.addAttribute("error", "이메일 또는 비밀번호가 맞지 않습니다.");
            return "login/LoginTemp";
        }

        session.setAttribute("LOGIN_USER_ID", user.getUserId());
        session.setAttribute("LOGIN_USER_NAME", user.getName());
        session.setAttribute("LOGIN_USER_ROLE", user.getRole());

        return "redirect:/home";
    }

}
