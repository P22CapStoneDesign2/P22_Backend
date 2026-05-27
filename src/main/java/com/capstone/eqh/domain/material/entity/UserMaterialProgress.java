package com.capstone.eqh.domain.material.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_material_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_material_progress",
                columnNames = {"user_id", "material_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMaterialProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "current_page", nullable = false)
    private int currentPage;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        this.updatedAt = LocalDateTime.now();
    }
}
