package com.learnit.learnit.quiz.service;

import com.learnit.learnit.quiz.dto.Quiz;
import com.learnit.learnit.quiz.repository.QuizMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizMapper quizMapper;

    // 사이드바용 리스트
    public List<Quiz> getQuizList(Long courseId) {
        return quizMapper.selectQuizListByCourseId(courseId);
    }

    // 퀴즈 상세 조회 (조건 검사 없이 바로 리턴)
    public Quiz getQuiz(Long quizId) {
        return quizMapper.selectQuizByQuizId(quizId);
    }

    // 파이널 퀴즈 ID만 뽑아오는 메서드
    public Long getFinalQuizId(Long courseId) {
        List<Quiz> quizList = quizMapper.selectQuizListByCourseId(courseId);

        return quizList.stream()
                .filter(q -> "FINAL".equals(q.getType())) // 타입이 FINAL인 것 찾기
                .findFirst()
                .map(Quiz::getQuizId) // 퀴즈 ID만 추출
                .orElse(null);        // 없으면 null
    }

    // 섹션별 퀴즈 Map 변환 로직
    public Map<String, Quiz> getQuizSectionMap(Long courseId) {
        List<Quiz> quizList = quizMapper.selectQuizListByCourseId(courseId);

        return quizList.stream()
                // 섹션 제목이 없는(FINAL 등) 경우는 제외하고 맵 생성
                .filter(q -> q.getSectionTitle() != null)
                .collect(Collectors.toMap(
                        Quiz::getSectionTitle,
                        q -> q,
                        (existing, replacement) -> existing
                ));
    }
}