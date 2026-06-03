package com.capstone.eqh.domain.lesson.repository;

import com.capstone.eqh.domain.lesson.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Page<Lesson> findAllByCreatedBy_Id(Long professorId, Pageable pageable);
}
