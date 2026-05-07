package com.capstone.eqh.domain.quiz.repository;

import com.capstone.eqh.domain.quiz.entity.Quiz;
import com.capstone.eqh.domain.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizOrderById(Quiz quiz);

    Optional<QuizQuestion> findByIdAndQuiz(Long id, Quiz quiz);
}
