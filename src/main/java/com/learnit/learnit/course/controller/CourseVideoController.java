package com.learnit.learnit.course.controller;

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

        // 현재 챕터 정보
        CourseVideo chapter = courseVideoService.getChapterDetail(chapterId);

        // 퀴즈 정보 로딩 (섹션 제목을 키값으로 Map 생성)
        List<Quiz> quizList = quizService.getQuizList(courseId);
        Map<String, Quiz> quizMap = quizList.stream()
                .collect(Collectors.toMap(Quiz::getSectionTitle, q -> q, (a, b) -> a));

        // 이전/다음 챕터 계산
        Long prevChapterId = courseVideoService.getPrevChapterId(courseId, chapter.getOrderIndex());
        Long nextChapterId = courseVideoService.getNextChapterId(courseId, chapter.getOrderIndex());

        // 다음 버튼이 퀴즈로 가야 하는지 판단
        boolean nextIsQuiz = false;
        Long nextQuizId = null;

        // 현재 섹션에 해당하는 퀴즈가 있는지 확인
        if (quizMap.containsKey(chapter.getSectionTitle())) {
            // 조건 1: 다음 영상이 아예 없거나 (코스 끝)
            // 조건 2: 다음 영상의 섹션이 현재와 다르다면 (섹션 끝)
            if (nextChapterId == null) {
                nextIsQuiz = true;
            } else {
                CourseVideo nextChapter = courseVideoService.getChapterDetail(nextChapterId);
                if (nextChapter != null && !nextChapter.getSectionTitle().equals(chapter.getSectionTitle())) {
                    nextIsQuiz = true;
                }
            }

            // 퀴즈로 가야 한다면 퀴즈 ID 설정
            if (nextIsQuiz) {
                nextQuizId = quizMap.get(chapter.getSectionTitle()).getQuizId();
            }
        }

        // 나머지 데이터
        int progressPercent = courseVideoService.getProgressPercent(1L, courseId);
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
                               @RequestBody Map<String, Object> payload) {
        Integer playTime = (Integer) payload.get("playTime");
        courseVideoService.saveStudyLog(1L, courseId, chapterId, playTime);
        return "ok";
    }

    @PostMapping("/course/log/duration")
    @ResponseBody
    public void updateDuration(@RequestParam Long chapterId, @RequestParam int duration) {
        courseVideoService.updateChapterDuration(chapterId, duration);
    }
}