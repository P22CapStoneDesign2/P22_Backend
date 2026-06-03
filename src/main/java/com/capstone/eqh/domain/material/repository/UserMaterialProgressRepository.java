package com.capstone.eqh.domain.material.repository;

import com.capstone.eqh.domain.material.entity.UserMaterialProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMaterialProgressRepository extends JpaRepository<UserMaterialProgress, Long> {

    Optional<UserMaterialProgress> findByUserIdAndMaterialId(Long userId, Long materialId);
}
