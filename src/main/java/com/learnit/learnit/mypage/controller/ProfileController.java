package com.learnit.learnit.mypage.controller;

import com.learnit.learnit.mypage.dto.ProfileDTO;
import com.learnit.learnit.mypage.dto.ProfileUpdateDTO;
import com.learnit.learnit.mypage.service.ProfileService;
import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserDTO user = userService.getUserDTOById(userId);
        model.addAttribute("user", user);

        // 프로필 정보 조회
        ProfileDTO profile = profileService.getProfile(userId);
        model.addAttribute("profile", profile);

        return "mypage/profile/profile_edit";
    }

    /**
     * 개인정보 수정 처리 (POST)
     */
    @PostMapping("/mypage/settings/update")
    public String updateProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "region", required = false) String region,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
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

            profileService.updateProfile(userId, updateDTO);
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
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
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
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        
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
}

