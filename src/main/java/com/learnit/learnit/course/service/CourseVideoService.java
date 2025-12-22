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
}
