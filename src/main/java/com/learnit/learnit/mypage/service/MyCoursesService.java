package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.CourseSummaryDTO;
import com.learnit.learnit.mypage.mapper.MyCoursesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCoursesService {

    private final MyCoursesMapper mypageCoursesMapper;

    /**
     * 사용자의 수강 중인 강의 목록을 조회합니다.
     * @param userId 사용자 ID
     * @return 강의 목록
     */
    public List<CourseSummaryDTO> getMyCourses(Long userId) {
        try {
            return mypageCoursesMapper.selectMyCourses(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
