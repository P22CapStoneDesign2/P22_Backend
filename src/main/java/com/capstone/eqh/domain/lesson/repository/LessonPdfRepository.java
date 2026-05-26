package com.capstone.eqh.domain.lesson.repository;

import com.capstone.eqh.domain.lesson.entity.LessonPdf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonPdfRepository extends JpaRepository<LessonPdf, Long> {

    List<LessonPdf> findByLessonId(Long lessonId);
}
