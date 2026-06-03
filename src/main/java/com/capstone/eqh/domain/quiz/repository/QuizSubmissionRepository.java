package com.capstone.eqh.domain.quiz.repository;

import com.capstone.eqh.domain.quiz.entity.Quiz;
import com.capstone.eqh.domain.quiz.entity.QuizSubmission;
import com.capstone.eqh.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    boolean existsByQuizAndStudent(Quiz quiz, User student);

    Optional<QuizSubmission> findByQuizAndStudent(Quiz quiz, User student);
}
