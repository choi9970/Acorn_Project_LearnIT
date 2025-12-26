package com.learnit.learnit.course.service;

import com.learnit.learnit.course.dto.CourseVideo;
import com.learnit.learnit.course.dto.CurriculumSection;
import com.learnit.learnit.course.repository.CourseVideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void updateChapterDuration(Long chapterId, int duration) {
        courseVideoMapper.updateChapterDuration(chapterId, duration);
    }

    public List<CurriculumSection> getCurriculumGrouped(Long courseId) {
        List<CourseVideo> allChapters = courseVideoMapper.selectChapterList(courseId);

        Map<String, List<CourseVideo>> grouped = allChapters.stream()
                .collect(Collectors.groupingBy(
                        ch -> ch.getSectionTitle() == null ? "기타" : ch.getSectionTitle(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CurriculumSection> result = new ArrayList<>();
        for (Map.Entry<String, List<CourseVideo>> entry : grouped.entrySet()) {
            result.add(new CurriculumSection(entry.getKey(), entry.getValue()));
        }

        return result;
    }
}
