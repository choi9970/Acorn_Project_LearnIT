package com.learnit.learnit.course.service;

import com.learnit.learnit.course.dto.CourseVideo;
import com.learnit.learnit.course.repository.CourseVideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseVideoService {
    private final CourseVideoMapper courseVideoMapper;

    public CourseVideo getChapterDetail(Long chapterId){
        return courseVideoMapper.findById(chapterId);
    }

    public Long getPrevChapterId(Long courseId, Long currentOrder) {
        return courseVideoMapper.selectPrevChapterId(courseId, currentOrder);
    }

    public Long getNextChapterId(Long courseId, Long currentOrder) {
        return courseVideoMapper.selectNextChapterId(courseId, currentOrder);
    }

    public void saveStudyLog(Long userId, Long courseId, Long chapterId, Integer playTime) {
        courseVideoMapper.insertOrUpdateStudyLog(userId, courseId, chapterId, playTime);
    }

    public int getProgressPercent(Long userId, Long courseId) {
        int total = courseVideoMapper.countTotalChapters(courseId);
        if (total == 0) return 0;
        int completed = courseVideoMapper.countCompletedChapters(userId, courseId);

        return (int) ((double) completed / total * 100);
    }
}
