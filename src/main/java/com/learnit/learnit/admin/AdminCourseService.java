package com.learnit.learnit.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final AdminCourseMapper adminCourseMapper;

    @Transactional(readOnly = true)
    public List<AdminCourse> getCourses(int page, int size, String status, String search) {
        int offset = (page - 1) * size;
        return adminCourseMapper.selectCourses(offset, size, status, search);
    }

    @Transactional(readOnly = true)
    public int getCourseCount(String status, String search) {
        return adminCourseMapper.countCourses(status, search);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        adminCourseMapper.deleteCourse(courseId);
    }
}
