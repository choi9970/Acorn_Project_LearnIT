package com.learnit.learnit.courseDetail;


import com.learnit.learnit.course.CourseDTO;
import com.learnit.learnit.user.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CourseDetailController {

    private final CourseDetailService courseDetailService;

    @GetMapping("/CourseDetail")
    public String detail(@RequestParam("courseId") int courseId,
                         @RequestParam(value = "tab", defaultValue = "intro") String tab,
                         Model model,
                         HttpSession session) {

        try {
            Long loginUserId = SessionUtils.getLoginUserId();
            boolean isLoggedIn = (loginUserId != null);

            // 강의 정보 가져오기
            CourseDTO course = courseDetailService.getCourse(courseId);
            if (course == null) {
                throw new IllegalArgumentException("Course not found. courseId=" + courseId);
            }

            // 관리자 권한 확인
            String role = (String) session.getAttribute("LOGIN_USER_ROLE");
            boolean isAdmin = "ADMIN".equals(role);
            boolean isSubAdmin = "SUB_ADMIN".equals(role);

            // 해당 강의 강사인지 확인
            boolean isInstructor = isSubAdmin && loginUserId != null && loginUserId.equals(Long.valueOf(course.getUserId()));

            // 관리자거나 해당 강의 강사라면 무조건 수강 중인 것으로 처리
            boolean isEnrolled = isLoggedIn && (isAdmin || isInstructor || courseDetailService.isEnrolled(loginUserId, courseId));

            setCommonModel(model, course, tab, isLoggedIn, isEnrolled, loginUserId);
            return "courseDetail/courseDetail";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "강의 상세 정보를 불러오는 중 오류가 발생했습니다.");
            return "error/500";
        }
    }

    private void setCommonModel(Model model,
                                CourseDTO course,
                                String tab,
                                boolean isLoggedIn,
                                boolean isEnrolled,
                                Long loginUserId) {

        int courseId = course.getCourseId();

        // ✅ DTO에 없는 화면용 값들
        String instructorName = courseDetailService.getInstructorNameByUserId(course.getUserId());
        String periodText = courseDetailService.getPeriodTextByCourseId(courseId);
        String categoryName = courseDetailService.getCategoryNameByCategoryId(course.getCategoryId());

        // ✅ null 방어 (화면 깨짐 방지)
        if (instructorName == null) instructorName = "지식공유자";
        if (periodText == null) periodText = "무제한";
        if (categoryName == null) categoryName = "카테고리";

        model.addAttribute("course", course);
        model.addAttribute("activeTab", tab);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("loginUserId", loginUserId);

        model.addAttribute("instructorName", instructorName);
        model.addAttribute("periodText", periodText);
        model.addAttribute("categoryName", categoryName);

        // 커리큘럼
        List<ChapterDTO> chapters = courseDetailService.getChapters(courseId);
        model.addAttribute("chapters", chapters);

        Map<String, List<ChapterDTO>> sectionMap = courseDetailService.getCurriculumSectionMap(courseId);
        model.addAttribute("sectionMap", sectionMap);

        model.addAttribute("curriculumTotal", chapters.size());

        // 리뷰(추후)
        //model.addAttribute("reviews", Collections.emptyList());
        model.addAttribute("courseId", courseId);
        // 이어보기 (수강 중일 때만)
        if (isEnrolled) {
            Long lastWatchedChapterId = courseDetailService.getLastWatchedChapterId(loginUserId, courseId);
            model.addAttribute("lastWatchedChapterId", lastWatchedChapterId);
        }
    }
}