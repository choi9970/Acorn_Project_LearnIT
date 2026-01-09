package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.MyCourseSummaryDTO;
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
     * 사용자의 수강 중인 강의 목록을 조회합니다. (페이징)
     * @param userId 사용자 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 당 개수
     * @return 강의 목록
     */
    public List<MyCourseSummaryDTO> getMyCourses(Long userId, int page, int size) {
        try {
            int offset = (page - 1) * size;
            return mypageCoursesMapper.selectMyCourses(userId, offset, size);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 사용자의 수강 중인 강의 총 개수 조회
     */
    public int getMyCoursesCount(Long userId) {
        try {
            return mypageCoursesMapper.countMyCourses(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
