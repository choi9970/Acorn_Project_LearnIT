package com.learnit.learnit.user.service;

import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.repository.UserRepository;
import com.learnit.learnit.user.dto.LoginRequestDTO;
import com.learnit.learnit.user.dto.SignupRequestDTO;
import com.learnit.learnit.user.dto.UserDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    /**
     * 로그인 처리
     */
    public User login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || user.getPassword() == null) {
            return null;
        }

        // 비밀번호 검증: BCrypt로 암호화된 경우와 평문 비밀번호 모두 지원
        String storedPassword = user.getPassword();
        boolean passwordMatches = false;
        
        // BCrypt로 암호화된 비밀번호인지 확인 (BCrypt 해시는 $2a$, $2b$, $2y$로 시작)
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            passwordMatches = passwordEncoder.matches(request.getPassword(), storedPassword);
        } else {
            // 평문 비밀번호인 경우 직접 비교 (null 체크 추가)
            if (request.getPassword() != null) {
                passwordMatches = request.getPassword().equals(storedPassword);
            } else {
                passwordMatches = false;
            }
        }

        if (!passwordMatches) {
            return null;
        }

        // SIGNUP_PENDING과 ACTIVE 상태만 로그인 허용 (BANNED, DELETE 차단)
        if (!User.STATUS_ACTIVE.equals(user.getStatus()) 
            && !User.STATUS_SIGNUP_PENDING.equals(user.getStatus())) {
            return null;
        }

        return user;
    }

    /**
     * 회원가입 처리
     */
    @Transactional
    public User signup(SignupRequestDTO request) {
        // 비밀번호 확인 검증
        if (request.getPasswordConfirm() == null || 
            !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 새 사용자 생성
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setName(request.getName());
        newUser.setNickname(request.getNickname() != null && !request.getNickname().isEmpty() 
                ? request.getNickname() : request.getName());
        newUser.setPhone(request.getPhone());
        newUser.setRegion(request.getRegion());
        
        // GitHub URL 처리 (사용자명만 입력받아서 URL로 변환)
        if (request.getGithubUrl() != null && !request.getGithubUrl().trim().isEmpty()) {
            String githubUrl = request.getGithubUrl().trim();
            // 이미 URL 형식이 아니면 사용자명으로 간주하고 URL 생성
            if (!githubUrl.startsWith("http://") && !githubUrl.startsWith("https://")) {
                if (!githubUrl.startsWith("github.com/")) {
                    githubUrl = "https://github.com/" + githubUrl;
                } else {
                    githubUrl = "https://" + githubUrl;
                }
            }
            newUser.setGithubUrl(githubUrl);
        }
        
        newUser.setRole("USER");
        newUser.setProvider("local"); // 일반 회원가입은 local로 설정
        
        // 일반 회원가입은 항상 ACTIVE 상태로 저장
        newUser.setStatus(User.STATUS_ACTIVE);
        
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        newUser = userRepository.save(newUser);
        return newUser;
    }

    /**
     * 이메일 중복 체크
     * @param email 확인할 이메일
     * @return 사용 가능하면 true, 중복이면 false
     */
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByEmail(email.trim());
    }

    /**
     * 세션에 로그인 정보 저장
     */
    public void setLoginSession(HttpSession session, User user) {
        sessionService.setLoginSession(session, user);
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    public User getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * 추가 정보 업데이트 및 상태 변경
     */
    @Transactional
    public void updateAdditionalInfo(Long userId, String nickname, String phone, String region, String githubUrl) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 유효성 검사
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        }
        
        // 추가 정보 업데이트 및 상태 변경
        user.setNickname(nickname.trim());
        user.setPhone(phone.trim());
        user.setRegion(region.trim());
        
        // GitHub URL 처리 (사용자명만 입력받아서 URL로 변환)
        if (githubUrl != null && !githubUrl.trim().isEmpty()) {
            String githubUrlFormatted = githubUrl.trim();
            // 이미 URL 형식이 아니면 사용자명으로 간주하고 URL 생성
            if (!githubUrlFormatted.startsWith("http://") && !githubUrlFormatted.startsWith("https://")) {
                if (!githubUrlFormatted.startsWith("github.com/")) {
                    githubUrlFormatted = "https://github.com/" + githubUrlFormatted;
                } else {
                    githubUrlFormatted = "https://" + githubUrlFormatted;
                }
            }
            user.setGithubUrl(githubUrlFormatted);
        } else {
            user.setGithubUrl(null);
        }
        
        user.setStatus(User.STATUS_ACTIVE); // 추가 정보 입력 완료 → ACTIVE
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * 사용자 ID로 UserDTO 조회
     */
    public UserDTO getUserDTOById(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        
        // profile_img가 null이거나 빈 문자열이거나 'profile-default.png'이면 null로 설정
        String profileImg = user.getProfileImg();
        if (profileImg == null || profileImg.isEmpty() || "profile-default.png".equals(profileImg)) {
            dto.setProfileImageUrl(null);
        } else {
            dto.setProfileImageUrl(profileImg);
        }
        
        return dto;
    }

    /**
     * 비밀번호 재설정 (임시 비밀번호 생성 및 저장)
     * 이메일 발송 성공 후에만 호출해야 함
     * @param email 사용자 이메일
     * @param tempPassword 임시 비밀번호
     */
    @Transactional
    public void resetPassword(String email, String tempPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        
        if (tempPassword == null || tempPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("임시 비밀번호가 없습니다.");
        }
        
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        
        if (user == null) {
            throw new IllegalArgumentException("등록되지 않은 이메일입니다.");
        }
        
        // 소셜 로그인 사용자는 비밀번호가 없을 수 있음
        if (user.getProvider() != null && !user.getProvider().isEmpty() && !user.getProvider().equals("local")) {
            throw new IllegalArgumentException("소셜 로그인으로 가입한 계정입니다. 소셜 로그인을 이용해주세요.");
        }
        
        // 비밀번호 암호화 후 저장
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * 사용자 이메일 검증 및 임시 비밀번호 생성
     * 이메일 발송 전에 호출하여 사용자 검증 및 임시 비밀번호 생성
     * @param email 사용자 이메일
     * @return 생성된 임시 비밀번호 (이메일 발송 성공 후 resetPassword 호출 필요)
     */
    public String preparePasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        
        if (user == null) {
            return null; // 등록되지 않은 이메일
        }
        
        // 소셜 로그인 사용자는 비밀번호가 없을 수 있음
        if (user.getProvider() != null && !user.getProvider().isEmpty() && !user.getProvider().equals("local")) {
            throw new IllegalArgumentException("소셜 로그인으로 가입한 계정입니다. 소셜 로그인을 이용해주세요.");
        }
        
        // 임시 비밀번호 생성 (8자리 영문+숫자 조합)
        return generateTempPassword();
    }
    
    /**
     * 임시 비밀번호 생성 (8자리 영문+숫자 조합)
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}

