package com.learnit.learnit.mypage.dashboard.service;

import com.learnit.learnit.mypage.dashboard.dto.TodoDTO;
import com.learnit.learnit.mypage.dashboard.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    /**
     * 날짜별 할일 목록 조회
     */
    public List<TodoDTO> getTodosByDate(Long userId, int year, int month, int day) {
        try {
            List<TodoDTO> todos = todoRepository.selectTodosByDate(userId, year, month, day);
            return todos != null ? todos : new java.util.ArrayList<>();
        } catch (Exception e) {
            // 로그 기록 후 빈 리스트 반환
            System.err.println("할일 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 할일 저장
     */
    @Transactional
    public TodoDTO saveTodo(TodoDTO todo) {
        try {
            if (todo.getTodoId() == null) {
                // 새 할일 생성
                if (todo.getIsCompleted() == null) {
                    todo.setIsCompleted(false);
                }
                
                // 필수 필드 검증
                if (todo.getTitle() == null || todo.getTitle().trim().isEmpty()) {
                    throw new IllegalArgumentException("할일 제목은 필수입니다.");
                }
                if (todo.getTargetDate() == null) {
                    throw new IllegalArgumentException("할일 날짜는 필수입니다.");
                }
                if (todo.getUserId() == null) {
                    throw new IllegalArgumentException("사용자 ID는 필수입니다.");
                }
                
                System.out.println("할일 저장 시작 - title: " + todo.getTitle() + ", targetDate: " + todo.getTargetDate() + ", userId: " + todo.getUserId());
                
                int result = todoRepository.insertTodo(todo);
                System.out.println("insertTodo 실행 결과: " + result + ", 생성된 todoId: " + todo.getTodoId());
                
                if (result == 0) {
                    throw new RuntimeException("할일 저장에 실패했습니다. (insert 결과: 0)");
                }
                
                // insertTodo 후 생성된 todoId 확인
                Long generatedTodoId = todo.getTodoId();
                System.out.println("생성된 todoId: " + generatedTodoId);
                
                if (generatedTodoId == null || generatedTodoId == 0) {
                    System.err.println("할일 저장 후 ID를 가져올 수 없습니다. insert 결과: " + result);
                    throw new RuntimeException("할일 저장 후 ID를 가져올 수 없습니다. (useGeneratedKeys 실패)");
                }
                
                // 생성된 todoId로 다시 조회하여 완전한 데이터 가져오기
                System.out.println("생성된 todoId로 조회 시도: " + generatedTodoId);
                TodoDTO savedTodo = todoRepository.selectTodoById(generatedTodoId, todo.getUserId());
                
                if (savedTodo == null) {
                    System.err.println("저장된 할일을 조회할 수 없습니다. todoId: " + generatedTodoId);
                    // 조회 실패해도 todoId는 있으므로, 최소한의 정보로 DTO 생성
                    savedTodo = new TodoDTO();
                    savedTodo.setTodoId(generatedTodoId);
                    savedTodo.setUserId(todo.getUserId());
                    savedTodo.setTitle(todo.getTitle());
                    savedTodo.setTargetDate(todo.getTargetDate());
                    savedTodo.setIsCompleted(todo.getIsCompleted());
                    System.out.println("할일 저장 성공 (부분 데이터): " + savedTodo.getTodoId());
                    return savedTodo;
                }
                
                System.out.println("할일 저장 성공: " + savedTodo.getTodoId());
                return savedTodo;
            } else {
                // 기존 할일 수정
                todoRepository.updateTodo(todo);
                TodoDTO updatedTodo = todoRepository.selectTodoById(todo.getTodoId(), todo.getUserId());
                if (updatedTodo == null) {
                    throw new RuntimeException("수정된 할일을 조회할 수 없습니다.");
                }
                return updatedTodo;
            }
        } catch (Exception e) {
            System.err.println("할일 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 할일 완료 처리
     */
    @Transactional
    public TodoDTO completeTodo(Long todoId, Long userId, boolean isCompleted) {
        TodoDTO todo = todoRepository.selectTodoById(todoId, userId);
        if (todo == null) {
            throw new RuntimeException("할일을 찾을 수 없습니다.");
        }
        
        todo.setIsCompleted(isCompleted);
        if (isCompleted) {
            todo.setCompletedAt(LocalDateTime.now());
        } else {
            todo.setCompletedAt(null);
        }
        
        todoRepository.updateTodo(todo);
        return todoRepository.selectTodoById(todoId, userId);
    }

    /**
     * 할일 삭제
     */
    @Transactional
    public void deleteTodo(Long todoId, Long userId) {
        int result = todoRepository.deleteTodo(todoId, userId);
        if (result == 0) {
            throw new RuntimeException("할일을 찾을 수 없습니다.");
        }
    }

    /**
     * 할일 일괄 저장 (날짜별)
     */
    @Transactional
    public void saveTodosBatch(Long userId, LocalDate targetDate, List<TodoDTO> todos) {
        // 기존 할일 삭제 후 새로 저장
        // 또는 기존 할일과 비교하여 업데이트/삭제/추가
        // 간단하게는 기존 할일을 모두 삭제하고 새로 저장
        for (TodoDTO todo : todos) {
            todo.setUserId(userId);
            todo.setTargetDate(targetDate);
            if (todo.getIsCompleted() == null) {
                todo.setIsCompleted(false);
            }
            if (todo.getTodoId() == null) {
                todoRepository.insertTodo(todo);
            } else {
                todoRepository.updateTodo(todo);
            }
        }
    }
}

