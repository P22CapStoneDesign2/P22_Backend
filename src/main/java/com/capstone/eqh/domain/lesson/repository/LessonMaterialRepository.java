package com.capstone.eqh.domain.lesson.repository;

import com.capstone.eqh.domain.lesson.entity.LessonMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, Long> {

    Page<LessonMaterial> findAllByLessonId(Long lessonId, Pageable pageable);
}
