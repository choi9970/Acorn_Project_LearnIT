package com.learnit.learnit.mypage.dashboard.repository;

import com.learnit.learnit.mypage.dashboard.dto.TodoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TodoRepository {
    // 날짜별 할일 조회
    List<TodoDTO> selectTodosByDate(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("day") int day
    );
    
    // 할일 저장
    int insertTodo(TodoDTO todo);
    
    // 할일 수정
    int updateTodo(TodoDTO todo);
    
    // 할일 삭제
    int deleteTodo(@Param("todoId") Long todoId, @Param("userId") Long userId);
    
    // 할일 ID로 조회
    TodoDTO selectTodoById(@Param("todoId") Long todoId, @Param("userId") Long userId);
}

