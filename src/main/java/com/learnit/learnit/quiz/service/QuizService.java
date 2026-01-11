package com.learnit.learnit.quiz.service;

import com.learnit.learnit.quiz.dto.Quiz;
import com.learnit.learnit.quiz.dto.QuizOption;
import com.learnit.learnit.quiz.dto.QuizSubmitRequest;
import com.learnit.learnit.quiz.repository.QuizMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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

    // 퀴즈 상세 조회 (단순 조회)
    public Quiz getQuiz(Long quizId) {
        return quizMapper.selectQuizByQuizId(quizId);
    }

    // 퀴즈 상세 조회 (제출 여부 포함)
    public Quiz getQuiz(Long quizId, Long userId) {
        Quiz quiz = quizMapper.selectQuizByQuizId(quizId);
        if (quiz != null && userId != null) {
            boolean submitted = isQuizSubmitted(userId, quizId);
            quiz.setSubmitted(submitted);
        }
        return quiz;
    }

    // 파이널 퀴즈 ID 조회
    public Long getFinalQuizId(Long courseId) {
        List<Quiz> quizList = quizMapper.selectQuizListByCourseId(courseId);
        
        // 반복문으로 직관적인 구현
        for (Quiz q : quizList) {
            if ("FINAL".equals(q.getType())) {
                return q.getQuizId();
            }
        }
        return null;
    }

    // 섹션별 퀴즈 Map 변환 로직
    public Map<String, Quiz> getQuizSectionMap(Long courseId) {
        List<Quiz> quizList = quizMapper.selectQuizListByCourseId(courseId);

        return quizList.stream()
                .filter(q -> q.getSectionTitle() != null)
                .collect(Collectors.toMap(
                        Quiz::getSectionTitle,
                        q -> q,
                        (existing, replacement) -> existing
                ));
    }

    // 퀴즈 응시 여부 확인
    public boolean isQuizSubmitted(Long userId, Long quizId) {
        if (userId == null || quizId == null) return false;
        return quizMapper.countUserQuizHistory(userId, quizId) > 0;
    }

    // 퀴즈 통과 여부 확인 (정답률 60% 이상)
    public boolean isQuizPassed(Long userId, Long quizId) {
        if (userId == null || quizId == null) return false;
        return quizMapper.isQuizPassed(userId, quizId);
    }

    // 강의의 모든 필수 퀴즈 통과 여부 확인
    public boolean areAllRequiredQuizzesPassed(Long userId, Long courseId) {
        if (userId == null || courseId == null) return false;
        
        List<Quiz> quizList = quizMapper.selectQuizListByCourseId(courseId);
        if (quizList == null || quizList.isEmpty()) {
            return true; // 퀴즈가 없으면 통과로 간주
        }
        
        // FINAL 퀴즈가 있다면 반드시 통과해야 함
        Long finalQuizId = getFinalQuizId(courseId);
        if (finalQuizId != null) {
            if (!isQuizSubmitted(userId, finalQuizId)) {
                return false; // FINAL 퀴즈를 아직 응시하지 않음
            }
            if (!isQuizPassed(userId, finalQuizId)) {
                return false; // FINAL 퀴즈를 통과하지 못함
            }
        }
        
        // 모든 퀴즈를 통과했는지 확인
        for (Quiz quiz : quizList) {
            // 퀴즈를 응시했다면 통과해야 함
            if (isQuizSubmitted(userId, quiz.getQuizId()) && !isQuizPassed(userId, quiz.getQuizId())) {
                return false; // 응시했지만 통과하지 못한 퀴즈가 있음
            }
        }
        
        return true;
    }

    @Transactional
    public void submitQuiz(Long userId, QuizSubmitRequest request) {
        if (userId == null || request == null) return;
        
        // 퀴즈 정보 조회
        Quiz quiz = quizMapper.selectQuizByQuizId(request.getQuizId());
        if (quiz == null) return;

        // 정답 정보 추출
        Map<Long, Long> correctAnswers = getCorrectAnswerMap(quiz);

        // 사용자 답안 저장
        for (QuizSubmitRequest.UserAnswerRequest ans : request.getAnswers()) {
            String isCorrect = "N";
            Long correctOptionId = correctAnswers.get(ans.getQuestionId());
            
            if (correctOptionId != null && correctOptionId.equals(ans.getOptionId())) {
                isCorrect = "Y";
            }

            quizMapper.insertUserAnswer(userId, ans.getQuestionId(), ans.getOptionId(), isCorrect);
        }
    }

    // 퀴즈 객체에서 문제별 정답(OptionId)을 추출하여 Map으로 반환
    private Map<Long, Long> getCorrectAnswerMap(Quiz quiz) {
        Map<Long, Long> map = new HashMap<>();
        
        for (var question : quiz.getQuestions()) {
            for (QuizOption option : question.getOptions()) {
                // "T" 또는 "Y"를 정답으로 간주
                if ("T".equals(option.getIsCorrect()) || "Y".equals(option.getIsCorrect())) {
                    map.put(question.getQuestionId(), option.getOptionId());
                }
            }
        }
        return map;
    }
}