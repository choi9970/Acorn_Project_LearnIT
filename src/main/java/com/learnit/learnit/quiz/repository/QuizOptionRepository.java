package com.learnit.learnit.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class QuizOptionRepository extends JpaRepository<QuizOption,Long> {

}
