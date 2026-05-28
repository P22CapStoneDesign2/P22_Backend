package com.capstone.eqh.domain.material.entity;

import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lecture_material")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Material extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "saved_file_name", nullable = false, length = 255)
    private String savedFileName;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Builder.Default
    @Column(name = "page_count", nullable = false)
    private int pageCount = 1;

    @Builder.Default
    @Column(name = "aspect_ratio", length = 32)
    private String aspectRatio = "0.707";

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Builder.Default
    @Column(name = "allow_download", nullable = false)
    private boolean allowDownload = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    public Long getLectureId() {
        return lesson.getId();
    }

    /** 업로드 직후 제목·메타 기본값 설정 */
    public void initViewerMetadata(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
