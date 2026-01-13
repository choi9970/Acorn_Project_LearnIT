package com.learnit.learnit.mypage.service;

import com.learnit.learnit.mypage.dto.MyCourseSummaryDTO;
import com.learnit.learnit.mypage.mapper.MyCoursesMapper;
import com.learnit.learnit.mypage.mapper.MyQnAMapper;
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
    private final MyQnAMapper qnAMapper;

    /**
     * 사용자의 수강 중인 강의 목록을 조회합니다. (페이징)
     * 관리자/서브어드민인 경우 분기 처리
     * @param userId 사용자 ID
     * @param userRole 사용자 권한
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 당 개수
     * @return 강의 목록
     */
    public List<MyCourseSummaryDTO> getMyCourses(Long userId, String userRole, int page, int size) {
        try {
            int offset = (page - 1) * size;
            
            // 관리자(ADMIN)인 경우: 모든 학습 강의 조회
            if ("ADMIN".equals(userRole)) {
                return mypageCoursesMapper.selectAdminCourses(offset, size);
            }
            // 서브 어드민(SUB_ADMIN)인 경우: 관리하는 강의의 학습 강의만 조회
            else if ("SUB_ADMIN".equals(userRole)) {
                List<Integer> managedCourseIds = qnAMapper.selectManagedCourseIds(userId);
                if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                    return new ArrayList<>();
                }
                return mypageCoursesMapper.selectSubAdminCourses(managedCourseIds, offset, size);
            }
            // 일반 사용자인 경우: 본인의 학습 강의만 조회
            else {
                return mypageCoursesMapper.selectMyCourses(userId, offset, size);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 사용자의 수강 중인 강의 총 개수 조회
     * 관리자/서브어드민인 경우 분기 처리
     */
    public int getMyCoursesCount(Long userId, String userRole) {
        try {
            // 관리자(ADMIN)인 경우: 모든 학습 강의 개수
            if ("ADMIN".equals(userRole)) {
                return mypageCoursesMapper.countAdminCourses();
            }
            // 서브 어드민(SUB_ADMIN)인 경우: 관리하는 강의의 학습 강의 개수
            else if ("SUB_ADMIN".equals(userRole)) {
                List<Integer> managedCourseIds = qnAMapper.selectManagedCourseIds(userId);
                if (managedCourseIds == null || managedCourseIds.isEmpty()) {
                    return 0;
                }
                return mypageCoursesMapper.countSubAdminCourses(managedCourseIds);
            }
            // 일반 사용자인 경우: 본인의 학습 강의 개수
            else {
                return mypageCoursesMapper.countMyCourses(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
