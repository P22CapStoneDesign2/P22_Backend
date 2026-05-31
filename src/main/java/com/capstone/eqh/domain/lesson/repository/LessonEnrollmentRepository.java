package com.capstone.eqh.domain.lesson.repository;

import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonEnrollmentRepository extends JpaRepository<LessonEnrollment, Long> {

    boolean existsByLessonIdAndStudentIdAndStatus(Long lessonId, Long studentId, EnrollmentStatus status);

    Optional<LessonEnrollment> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    @Query("""
            select e.lesson.id from LessonEnrollment e
            where e.student.id = :studentId
              and e.status = :status
            """)
    List<Long> findLessonIdsByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status);

    @Query("""
            select e from LessonEnrollment e
            where e.lesson.id = :lessonId
            """)
    Page<LessonEnrollment> findAllByLessonId(@Param("lessonId") Long lessonId, Pageable pageable);

    @Query("""
            select e from LessonEnrollment e
            where e.lesson.id = :lessonId
              and e.status = :status
            """)
    Page<LessonEnrollment> findAllByLessonIdAndStatus(
            @Param("lessonId") Long lessonId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable);

    @Query("""
            select e from LessonEnrollment e
            join fetch e.lesson l
            left join fetch l.createdBy
            where e.student.id = :studentId
              and e.status = :status
            """)
    Page<LessonEnrollment> findAllByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable);
}
