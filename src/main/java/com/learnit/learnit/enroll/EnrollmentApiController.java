package com.learnit.learnit.enroll;

import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class EnrollmentApiController {

    private final EnrollmentMapper enrollmentMapper;

    @GetMapping("/api/enrollmentsIds")
    public List<Long> activeEnrollmentCourseIds(HttpSession session) {
        Long userId = SessionUtils.getUserId(session);
        if (userId == null) {
            // ✅ 비로그인이어도 JSON [] 로 응답
            return Collections.emptyList();
        }

        List<Long> ids = enrollmentMapper.selectActiveCourseIds(userId);
        return ids == null ? Collections.emptyList() : ids;
    }
}
