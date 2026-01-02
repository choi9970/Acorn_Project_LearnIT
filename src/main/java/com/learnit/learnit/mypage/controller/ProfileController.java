package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.user.util.SessionUtils;
import com.learnit.learnit.mypage.dto.ProfileDTO;
import com.learnit.learnit.mypage.dto.ProfileUpdateDTO;
import com.learnit.learnit.mypage.service.ProfileService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;

    /**
     * 개인정보 수정 페이지 (GET)
     */
    @GetMapping("/mypage/settings")
    public String settingsPage(Model model, HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);

        // 프로필 정보 조회
        ProfileDTO profile = profileService.getProfile(userId);
        model.addAttribute("profile", profile);
        
        // 소셜 로그인 여부 확인을 위해 provider 정보 추가
        User userEntity = userService.getUserById(userId);
        if (userEntity != null) {
            boolean isSocialLogin = userEntity.getProvider() != null && 
                !userEntity.getProvider().isEmpty() && 
                !userEntity.getProvider().equals("local");
            model.addAttribute("isSocialLogin", isSocialLogin);
        } else {
            model.addAttribute("isSocialLogin", false);
        }

        return "mypage/profile/profile_edit";
    }

    /**
     * 개인정보 수정 처리 (POST)
     */
    @PostMapping("/mypage/settings/update")
    public String updateProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "currentPassword", required = false) String currentPassword,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "region", required = false) String region,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
            updateDTO.setName(name);
            updateDTO.setPassword(password);
            updateDTO.setEmail(email);
            updateDTO.setPhone(phone);
            updateDTO.setGithubUrl(githubUrl);
            updateDTO.setRegion(region);

            profileService.updateProfile(userId, updateDTO, currentPassword);
            redirectAttributes.addFlashAttribute("successMessage", "개인정보가 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mypage/settings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "개인정보 수정 중 오류가 발생했습니다.");
            return "redirect:/mypage/settings";
        }

        return "redirect:/mypage/settings";
    }

    /**
     * 프로필 이미지 업로드 (AJAX)
     */
    @PostMapping("/mypage/settings/upload-profile-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            response.put("success", false);
            response.put("error", "로그인이 필요합니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String imageUrl = profileService.updateProfileImage(userId, file);
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 프로필 이미지 제거 (AJAX)
     */
    @PostMapping("/mypage/settings/remove-profile-image")
    public ResponseEntity<Map<String, Object>> removeProfileImage(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Long userId = SessionUtils.getUserId(session);
        
        if (userId == null) {
            response.put("success", false);
            response.put("error", "로그인이 필요합니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            profileService.removeProfileImage(userId);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "이미지 제거 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 회원 탈퇴 처리
     */
    @PostMapping("/mypage/settings/withdraw")
    public String withdrawUser(HttpSession session) {
        // 세션과 SessionUtils 모두 시도 (OAuth 사용자 대응)
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            userId = SessionUtils.getLoginUserId();
        }
        
        log.info("회원 탈퇴 요청 - userId: {}", userId);
        
        if (userId == null) {
            log.warn("회원 탈퇴 실패: 로그인된 사용자를 찾을 수 없습니다.");
            return "redirect:/login";
        }

        try {
            log.info("회원 탈퇴 처리 시작 - userId: {}", userId);
            
            profileService.withdrawUser(userId);
            
            log.info("회원 탈퇴 처리 완료 - userId: {}", userId);
            
            // 세션 무효화
            session.invalidate();
            
            return "redirect:/home?withdrawn=true";
        } catch (IllegalArgumentException e) {
            log.error("회원 탈퇴 실패 (IllegalArgumentException): userId={}, error={}", userId, e.getMessage(), e);
            // 에러 발생 시 세션은 유지하고 설정 페이지로 리다이렉트
            return "redirect:/mypage/settings?error=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("회원 탈퇴 실패 (Exception): userId={}, error={}", userId, e.getMessage(), e);
            return "redirect:/mypage/settings?error=" + java.net.URLEncoder.encode("탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}

