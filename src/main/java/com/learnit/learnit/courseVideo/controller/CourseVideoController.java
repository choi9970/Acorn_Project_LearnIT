package com.learnit.learnit.courseVideo.controller;

import com.learnit.learnit.courseVideo.dto.CourseFile;
import com.learnit.learnit.courseVideo.dto.CourseVideo;
import com.learnit.learnit.courseVideo.dto.CurriculumSection;
import com.learnit.learnit.courseVideo.service.CourseVideoService;
import com.learnit.learnit.quiz.dto.Quiz;
import com.learnit.learnit.quiz.service.QuizService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CourseVideoController {
    private final CourseVideoService courseVideoService;
    private final QuizService quizService; // 퀴즈 서비스 추가

    //영상 화면
    @GetMapping("/course/play")
    public String playCourseVideo(@RequestParam("courseId") Long courseId,
                                  @RequestParam("chapterId") Long chapterId,
                                  HttpSession session,
                                  Model model) {

        //로그인 및 수강 권한 체크
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return "redirect:/login";

        // 관리자 여부 체크 (관리자는 수강권한 없이도 접근 가능)
        String role = (String) session.getAttribute("LOGIN_USER_ROLE");
        boolean isAdmin = "ADMIN".equals(role);
        boolean isSubAdmin = "SUB_ADMIN".equals(role);

        boolean isEnrolled = courseVideoService.isUserEnrolled(userId, courseId);
        boolean isInstructor = isSubAdmin && courseVideoService.isInstructor(userId, courseId);

        // 관리자도 아니고, 해당 강의 강사도 아니고, 수강생도 아니면 접근 불가 -> 결제 페이지로 이동
        if (!isAdmin && !isInstructor && !isEnrolled) {
            return "redirect:/payment?courseIds=" + courseId;
        }

        // 현재 챕터 정보 로딩
        CourseVideo chapter = courseVideoService.getChapterDetail(chapterId);

        // 퀴즈 정보 로딩
        Map<String, Quiz> quizMap = quizService.getQuizSectionMap(courseId);
        Long finalQuizId = quizService.getFinalQuizId(courseId);

        // 네비게이션 계산 (이전/다음)
        Long prevChapterId = courseVideoService.getPrevChapterId(courseId, chapter.getOrderIndex());
        Long nextChapterId = courseVideoService.getNextChapterId(courseId, chapter.getOrderIndex());

        // 다음 퀴즈 ID 계산 (중간 퀴즈용)
        Long nextQuizId = courseVideoService.getNextQuizId(chapter, nextChapterId, quizMap);
        boolean nextIsQuiz = (nextQuizId != null);

        //파이널 퀴즈 응시 여부 확인
        boolean isFinalSubmitted = false;
        if (finalQuizId != null) {
            isFinalSubmitted = quizService.isQuizSubmitted(userId, finalQuizId);
        }

        // 기타 데이터 (진도율, 커리큘럼)
        int progressPercent = courseVideoService.getProgressPercent(userId, courseId);
        List<CurriculumSection> curriculum = courseVideoService.getCurriculumGrouped(courseId);

        model.addAttribute("chapter", chapter);
        model.addAttribute("courseId", courseId);
        model.addAttribute("prevChapterId", prevChapterId);
        model.addAttribute("nextChapterId", nextChapterId);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("curriculum", curriculum);

        model.addAttribute("quizMap", quizMap);
        model.addAttribute("nextIsQuiz", nextIsQuiz);
        model.addAttribute("nextQuizId", nextQuizId);
        model.addAttribute("finalQuizId", finalQuizId);
        model.addAttribute("isFinalSubmitted", isFinalSubmitted);

        return "courseVideo/courseVideo";
    }

    // 진도율 저장 로그
    @PostMapping("/course/log")
    @ResponseBody
    public String saveProgress(@RequestParam("courseId") Long courseId,
                               @RequestParam("chapterId") Long chapterId,
                               @RequestBody Map<String, Object> payload,
                               HttpSession session) {

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (userId == null) {
            return "login_required"; // 에러 내지 말고 문자열 반환
        }

        Integer playTime = (Integer) payload.get("playTime");
        courseVideoService.saveStudyLog(userId, courseId, chapterId, playTime);
        return "ok";
    }

    @PostMapping("/course/log/duration")
    @ResponseBody
    public void updateDuration(@RequestParam Long chapterId, @RequestParam int duration) {
        courseVideoService.updateChapterDuration(chapterId, duration);
    }

    // 자료실 리스트를 주는 API 추가
    @GetMapping("/api/resources")
    @ResponseBody
    public List<CourseFile> getResources(@RequestParam("courseId") Long courseId) {
        return courseVideoService.getCourseResources(courseId);
    }
}