package com.learnit.learnit.course.controller;

import com.learnit.learnit.course.dto.CourseFile;
import com.learnit.learnit.course.dto.CourseVideo;
import com.learnit.learnit.course.dto.CurriculumSection;
import com.learnit.learnit.course.service.CourseVideoService;
import com.learnit.learnit.quiz.dto.Quiz;
import com.learnit.learnit.quiz.service.QuizService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CourseVideoController {
    private final CourseVideoService courseVideoService;
    private final QuizService quizService; // 퀴즈 서비스 추가

    @GetMapping("/course/play")
    public String playCourseVideo(@RequestParam("courseId") Long courseId,
                                  @RequestParam("chapterId") Long chapterId,
                                  HttpSession session,
                                  Model model) {

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        // 현재 챕터 정보
        CourseVideo chapter = courseVideoService.getChapterDetail(chapterId);

        // 퀴즈 정보 로딩 (섹션 제목을 키값으로 Map 생성)
        List<Quiz> quizList = quizService.getQuizList(courseId);
        Map<String, Quiz> quizMap = quizList.stream()
                .collect(Collectors.toMap(Quiz::getSectionTitle, q -> q, (a, b) -> a));

        // 이전/다음 챕터 계산
        Long prevChapterId = courseVideoService.getPrevChapterId(courseId, chapter.getOrderIndex());
        Long nextChapterId = courseVideoService.getNextChapterId(courseId, chapter.getOrderIndex());
        Long nextQuizId = courseVideoService.getNextQuizId(chapter, nextChapterId, quizMap);

        boolean nextIsQuiz = (nextQuizId != null);

        // 나머지 데이터
        int progressPercent = courseVideoService.getProgressPercent(userId, courseId);
        List<CurriculumSection> curriculum = courseVideoService.getCurriculumGrouped(courseId);

        // 모델 담기
        model.addAttribute("chapter", chapter);
        model.addAttribute("courseId", courseId);
        model.addAttribute("prevChapterId", prevChapterId);
        model.addAttribute("nextChapterId", nextChapterId);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("curriculum", curriculum);

        // 퀴즈 관련 데이터
        model.addAttribute("quizMap", quizMap);
        model.addAttribute("nextIsQuiz", nextIsQuiz);
        model.addAttribute("nextQuizId", nextQuizId);

        return "course/courseVideo";
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