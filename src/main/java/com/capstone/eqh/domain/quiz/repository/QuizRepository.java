package com.capstone.eqh.domain.quiz.repository;

import com.capstone.eqh.domain.quiz.entity.Quiz;
import com.capstone.eqh.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Page<Quiz> findByProfessor(User professor, Pageable pageable);

    Page<Quiz> findByProfessor_Id(Long professorId, Pageable pageable);
}
