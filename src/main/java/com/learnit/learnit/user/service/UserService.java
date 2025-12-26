package com.learnit.learnit.user.service;

import com.learnit.learnit.user.entity.User;
import com.learnit.learnit.user.mapper.UserMapper;
import com.learnit.learnit.user.dto.LoginRequestDTO;
import com.learnit.learnit.user.dto.SignupRequestDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인 처리
     */
    public User login(LoginRequestDTO request) {
        User user = userMapper.selectUserByEmail(request.getEmail());

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
            // 평문 비밀번호인 경우 직접 비교
            passwordMatches = request.getPassword().equals(storedPassword);
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
        // 이메일 중복 체크
        User existingUser = userMapper.selectUserByEmail(request.getEmail());
        if (existingUser != null) {
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
        newUser.setRole("USER");
        
        // 추가 정보가 완료되었으면 ACTIVE, 아니면 SIGNUP_PENDING
        if (newUser.isAdditionalInfoCompleted()) {
            newUser.setStatus(User.STATUS_ACTIVE);
        } else {
            newUser.setStatus(User.STATUS_SIGNUP_PENDING);
        }
        
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        userMapper.insertUser(newUser);
        return newUser;
    }

    /**
     * 세션에 로그인 정보 저장
     */
    public void setLoginSession(HttpSession session, User user) {
        session.setAttribute("LOGIN_USER_ID", user.getUserId());
        session.setAttribute("LOGIN_USER_NAME", user.getName());
        session.setAttribute("LOGIN_USER_ROLE", user.getRole());
    }
}

