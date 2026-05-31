package com.capstone.eqh.domain.material.repository;

import com.capstone.eqh.domain.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByLesson_IdOrderByCreatedAtDesc(Long lessonId);
}
