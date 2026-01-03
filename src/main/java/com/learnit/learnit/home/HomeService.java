package com.learnit.learnit.home;

import com.learnit.learnit.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final CourseMapper courseMapper;

    //인기강의 불러오기(최대 6개 제한)
    public List<Map<String, Object>> getPopularCourseList(){
        return courseMapper.selectPopularCourse(5);
    }

    //썸네일배너 강의 불러오기(최대 4개 제한)
    public List<Map<String, Object>> getBannerCourse(){
        return courseMapper.selectBannerCourse(4);
    }
}
