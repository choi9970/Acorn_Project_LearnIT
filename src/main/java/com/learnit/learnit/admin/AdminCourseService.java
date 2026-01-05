package com.learnit.learnit.admin;

import com.learnit.learnit.user.dto.UserDTO;
import com.learnit.learnit.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final AdminCourseMapper adminCourseMapper;
    private final UserMapper userMapper;

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

    @Transactional
    public void createCourse(AdminCourseCreateDTO dto) {
        // 상시 오픈 체크 시 날짜 NULL 처리
        if (dto.isAlwaysOpen()) {
            dto.setStartDate(null);
            dto.setEndDate(null);
        }
        adminCourseMapper.insertCourse(dto);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> searchInstructors(String keyword) {
        return userMapper.searchInstructors(keyword);
    }
}
