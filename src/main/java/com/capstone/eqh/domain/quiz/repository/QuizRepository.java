package com.capstone.eqh.domain.quiz.repository;

import com.capstone.eqh.domain.quiz.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Page<Quiz> findByProfessor_Id(Long professorId, Pageable pageable);

    Page<Quiz> findByProfessor_IdAndMaterial_Id(Long professorId, Long materialId, Pageable pageable);

    Page<Quiz> findByMaterial_Id(Long materialId, Pageable pageable);

    Page<Quiz> findByMaterial_Lesson_IdIn(Collection<Long> lessonIds, Pageable pageable);

    List<Quiz> findAllByMaterial_Id(Long materialId);
}
