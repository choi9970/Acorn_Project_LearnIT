package com.learnit.learnit.home;

import com.learnit.learnit.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final CourseMapper courseMapper;

    //인기강의 불러오기(최대 6개 제한)
    public List<MainCourse> getPopularCourseList(){
        return courseMapper.selectPopularCourse(5);
    }

    //썸네일배너 강의 불러오기(최대 4개 제한)
    public List<MainCourse> getBannerCourse(){
        return courseMapper.selectBannerCourse(4);
    }
}
