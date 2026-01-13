package com.learnit.learnit.user.controller;

import com.learnit.learnit.cart.CartService;
import com.learnit.learnit.user.dto.LoginRequestDTO;
import com.learnit.learnit.user.dto.SignupRequestDTO;
import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.service.EmailService;
import com.learnit.learnit.user.service.SessionService;
import com.learnit.learnit.user.service.UserService;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SessionService sessionService;
    private final EmailService emailService;
    private final CartService cartService;

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String redirect,
            Model model,
            HttpSession session
    ) {
        // ✅ redirect가 있으면 세션에 저장 (로그인 성공 후 이동용)
        if (redirect != null && !redirect.isBlank()) {
            session.setAttribute("REDIRECT_AFTER_LOGIN", redirect);
        }

        if (error != null) {
            String oauthError = (String) session.getAttribute("oauthError");
            if (oauthError != null) {
                model.addAttribute("error", oauthError);
                session.removeAttribute("oauthError");
            } else {
                model.addAttribute("error", "로그인에 실패했습니다.");
            }
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

        @SuppressWarnings("unchecked")
        List<Long> guestCourseIds =
                (List<Long>) session.getAttribute("GUEST_CART_COURSE_IDS");

        if (guestCourseIds != null && !guestCourseIds.isEmpty()) {
            cartService.mergeGuestCartToUser(user.getUserId(), guestCourseIds);
            session.removeAttribute("GUEST_CART_COURSE_IDS");
        }

        // ✅✅ 로그인 직후 DB 장바구니 정리(이미 수강중 강의 제거)
        cartService.deleteEnrolledCoursesFromCart(user.getUserId());

        // 관리자 계정인 경우 메인 페이지로 리다이렉트 - ADMIN 또는 SUB_ADMIN
        String userRole = user.getRole();
        if (userRole != null && ("ADMIN".equals(userRole.trim()) || "SUB_ADMIN".equals(userRole.trim()))) {
            // 관리자는 redirect 파라미터 무시하고 항상 메인 페이지로
            session.removeAttribute("REDIRECT_AFTER_LOGIN");
            return "redirect:/home";
        }

        // ✅ redirect 우선 (안전한 경로만 허용)
        String redirect = (String) session.getAttribute("REDIRECT_AFTER_LOGIN");
        session.removeAttribute("REDIRECT_AFTER_LOGIN");

        if (isSafeRedirect(redirect)) {
            return "redirect:" + redirect;
        }

        return "redirect:/home";
    }

    // ✅ 외부 URL 오픈리다이렉트 방지
    private boolean isSafeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank()) return false;
        // 내부 경로만 허용
        if (!redirect.startsWith("/")) return false;
        // // 또는 http 포함 등 차단
        if (redirect.startsWith("//")) return false;
        if (redirect.contains("http://") || redirect.contains("https://")) return false;
        return true;
    }

    @GetMapping("/signup")
    public String showSignupForm() {
        return "user/signup";
    }

    /**
     * 이메일 중복 체크 API
     */
    @GetMapping("/api/user/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        // 이메일 형식 검증
        if (email == null || email.trim().isEmpty()) {
            response.put("available", false);
            response.put("message", "이메일을 입력해주세요.");
            return ResponseEntity.ok(response);
        }

        String emailPattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        if (!email.matches(emailPattern)) {
            response.put("available", false);
            response.put("message", "올바른 이메일 형식을 입력해주세요.");
            return ResponseEntity.ok(response);
        }

        // 이메일 중복 체크
        boolean available = userService.isEmailAvailable(email);
        if (available) {
            response.put("available", true);
            response.put("message", "사용 가능한 이메일입니다.");
        } else {
            response.put("available", false);
            response.put("message", "이미 사용 중인 이메일입니다.");
        }

        return ResponseEntity.ok(response);
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
            @RequestParam(required = false) String githubUrl,
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
        request.setGithubUrl(githubUrl);

        // 비즈니스 로직은 Service에서 처리
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
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            return "redirect:/login";
        }

        // ACTIVE 상태면 이미 가입 완료된 사용자이므로 홈으로 리다이렉트
        if (User.STATUS_ACTIVE.equals(user.getStatus())) {
            // 관리자 계정인 경우 메인 페이지로 리다이렉트 - ADMIN 또는 SUB_ADMIN
            String userRole = user.getRole();
            if (userRole != null && ("ADMIN".equals(userRole.trim()) || "SUB_ADMIN".equals(userRole.trim()))) {
                return "redirect:/home";
            }
            return "redirect:/home";
        }

        // SIGNUP_PENDING 상태만 추가 정보 입력 페이지 표시
        return "user/additionalInfo";
    }

    @PostMapping("/user/additional-info")
    public String submitAdditionalInfo(
            @RequestParam String nickname,
            @RequestParam String phone,
            @RequestParam String region,
            @RequestParam(required = false) String githubUrl,
            HttpSession session,
            Model model
    ) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        // 비즈니스 로직은 Service에서 처리
        try {
            // 추가 정보 업데이트 및 상태를 ACTIVE로 변경
            userService.updateAdditionalInfo(userId, nickname, phone, region, githubUrl);

            // 업데이트된 사용자 정보 조회
            User updatedUser = userService.getUserById(userId);
            if (updatedUser == null) {
                return "redirect:/login";
            }

            // 세션 갱신 (ACTIVE 상태로 변경된 사용자 정보로)
            sessionService.setLoginSession(session, updatedUser);

            // 관리자 계정인 경우 메인 페이지로 리다이렉트 - ADMIN 또는 SUB_ADMIN
            String userRole = updatedUser.getRole();
            if (userRole != null && ("ADMIN".equals(userRole.trim()) || "SUB_ADMIN".equals(userRole.trim()))) {
                return "redirect:/home";
            }

            return "redirect:/home";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "user/additionalInfo";
        }
    }

    @GetMapping("/user/find-password")
    public String showFindPasswordForm(Model model) {
        // 초기값 설정 (템플릿에서 null 체크 오류 방지)
        if (!model.containsAttribute("success")) {
            model.addAttribute("success", null);
        }
        if (!model.containsAttribute("error")) {
            model.addAttribute("error", null);
        }
        return "user/findPassword";
    }

    @PostMapping("/user/find-password")
    public String findPassword(
            @RequestParam String email,
            Model model
    ) {
        try {
            // 1. 사용자 검증 및 임시 비밀번호 생성 (비밀번호는 아직 변경하지 않음)
            String tempPassword = userService.preparePasswordReset(email);

            if (tempPassword != null) {
                // 2. 이메일 발송 시도 (발송 성공 시에만 비밀번호 변경)
                try {
                    emailService.sendTempPasswordEmail(email, tempPassword);

                    // 3. 이메일 발송 성공 시에만 비밀번호 변경
                    userService.resetPassword(email, tempPassword);

                    model.addAttribute("success",
                            "임시 비밀번호가 이메일로 발송되었습니다. 이메일을 확인하신 후, 로그인 페이지에서 이메일과 임시 비밀번호를 입력하여 로그인해주세요.");
                } catch (Exception e) {
                    // 이메일 발송 실패 시 비밀번호는 변경하지 않음 (롤백)
                    model.addAttribute("error",
                            "이메일 발송에 실패했습니다. 관리자에게 문의해주세요.");
                }
            } else {
                model.addAttribute("error", "등록되지 않은 이메일입니다.");
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "비밀번호 찾기 중 오류가 발생했습니다.");
        }

        return "user/findPassword";
    }

    @PostMapping("/logout")
    public String doLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/home";
    }
}
