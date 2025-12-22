package com.learnit.learnit.course.controller;

import com.learnit.learnit.course.dto.CourseVideo;
import com.learnit.learnit.course.service.CourseVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CourseVideoController {
    private final CourseVideoService courseVideoService;

    // 1. 강의 재생 화면
    @GetMapping("/course/{courseId}/video/{chapterId}")
    public String playCourseVideo(@PathVariable Long courseId,
                                  @PathVariable Long chapterId,
                                  Model model){

        // 현재 챕터 정보 가져오기
        CourseVideo chapter = courseVideoService.getChapterDetail(chapterId);

        // 유효성 검사 (잘못된 접근 차단)
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            return "redirect:/error/404";
        }

        // 이전/다음 챕터 구하기
        Long prevChapterId = courseVideoService.getPrevChapterId(courseId, chapter.getOrderIndex());
        Long nextChapterId = courseVideoService.getNextChapterId(courseId, chapter.getOrderIndex());

        // 모델에 담기
        model.addAttribute("chapter", chapter);
        model.addAttribute("courseId", courseId);
        model.addAttribute("prevChapterId", prevChapterId);
        model.addAttribute("nextChapterId", nextChapterId);

        return "course/courseVideo";
    }

    // 2. [추가] 진도율 DB 저장 (Ajax 요청)
    @PostMapping("/course/{courseId}/video/{chapterId}/log")
    @ResponseBody
    public String saveProgress(@PathVariable Long courseId,
                               @PathVariable Long chapterId,
                               @RequestBody Map<String, Object> payload) {

        // Map에서 시간 꺼내기 (JSON -> Java)
        Integer playTime = (Integer) payload.get("playTime");

        // [임시] 로그인 기능 전이므로 '1번 유저'로 고정
        Long userId = 1L;

        // 로그 확인
        System.out.println(" DB 저장 요청: 유저" + userId + " / 챕터" + chapterId + " / 시간" + playTime + "초");

        // 서비스 호출 -> DB 저장
        courseVideoService.saveStudyLog(userId, courseId, chapterId, playTime);

        return "ok";
    }
}