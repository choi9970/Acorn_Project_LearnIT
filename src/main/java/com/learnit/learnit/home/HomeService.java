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
        List<Map<String, Object>> list = courseMapper.selectPopularCourse(5);
        if (list != null) {
            for (Map<String, Object> map : list) {
                if (!map.containsKey("thumbnailUrl") || map.get("thumbnailUrl") == null) {
                    map.put("thumbnailUrl", ""); // 기본값 설정 (빈 문자열)
                }
            }
        }
        return list;
    }

    //썸네일배너 강의 불러오기(최대 4개 제한)
    public List<Map<String, Object>> getBannerCourse(){
        List<Map<String, Object>> list = courseMapper.selectBannerCourse(4);
        if (list != null) {
            for (Map<String, Object> map : list) {
                if (!map.containsKey("thumbnailUrl") || map.get("thumbnailUrl") == null) {
                    map.put("thumbnailUrl", ""); // 기본값 설정 (빈 문자열)
                }
            }
        }
        return list;
    }
}
