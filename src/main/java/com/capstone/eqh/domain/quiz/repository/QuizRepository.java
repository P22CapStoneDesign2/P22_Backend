package com.capstone.eqh.domain.quiz.repository;

import com.capstone.eqh.domain.quiz.entity.Quiz;
import com.capstone.eqh.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Page<Quiz> findByProfessor(User professor, Pageable pageable);

    Page<Quiz> findByProfessor_Id(Long professorId, Pageable pageable);

    Page<Quiz> findByProfessor_IdAndLesson_Id(Long professorId, Long lessonId, Pageable pageable);

    Page<Quiz> findByLesson_Id(Long lessonId, Pageable pageable);

    Page<Quiz> findByLesson_IdIn(Collection<Long> lessonIds, Pageable pageable);
}
