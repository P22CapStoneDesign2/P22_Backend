package com.capstone.eqh.domain.lesson.entity;

import com.capstone.eqh.domain.material.entity.Material;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SQLDelete(sql = "UPDATE lesson SET deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted = false")
public class Lesson extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private User createdBy;

    @OneToMany(mappedBy = "lesson")
    @Builder.Default
    private List<Material> pdfs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonMaterial> materials = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
