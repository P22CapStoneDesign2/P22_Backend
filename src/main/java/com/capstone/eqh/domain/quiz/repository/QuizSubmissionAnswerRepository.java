package com.capstone.eqh.domain.quiz.repository;

import com.capstone.eqh.domain.quiz.entity.QuizSubmissionAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizSubmissionAnswerRepository extends JpaRepository<QuizSubmissionAnswer, Long> {

    @Query(value = "SELECT sa FROM QuizSubmissionAnswer sa " +
            "WHERE sa.submission.student.id = :studentId AND sa.correct = false",
            countQuery = "SELECT COUNT(sa) FROM QuizSubmissionAnswer sa " +
            "WHERE sa.submission.student.id = :studentId AND sa.correct = false")
    Page<QuizSubmissionAnswer> findWrongAnswersByStudentId(@Param("studentId") Long studentId, Pageable pageable);
}
