package com.learnit.learnit.category;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryRepository {

    @Autowired
    SqlSession session;

    public List<CategoryDTO> selectAll() {
        // 여기 namespace + id 가 XML이랑 똑같아야 함
        return session.selectList("com.learnit.learnit.category.CategoryRepository.findAll");
    }
}