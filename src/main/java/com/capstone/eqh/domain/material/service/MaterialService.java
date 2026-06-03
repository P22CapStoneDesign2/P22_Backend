package com.capstone.eqh.domain.material.service;

import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.lesson.repository.LessonEnrollmentRepository;
import com.capstone.eqh.domain.lesson.repository.LessonRepository;
import com.capstone.eqh.domain.material.dto.request.ProgressRequest;
import com.capstone.eqh.domain.material.dto.response.MaterialDetailResponse;
import com.capstone.eqh.domain.material.dto.response.MaterialListResponse;
import com.capstone.eqh.domain.material.dto.response.PageResponse;
import com.capstone.eqh.domain.material.dto.response.ViewerResponse;
import com.capstone.eqh.domain.material.entity.Material;
import com.capstone.eqh.domain.material.entity.UserMaterialProgress;
import com.capstone.eqh.domain.material.exception.MaterialAccessDeniedException;
import com.capstone.eqh.domain.material.exception.MaterialNotFoundException;
import com.capstone.eqh.domain.material.repository.MaterialRepository;
import com.capstone.eqh.domain.material.repository.UserMaterialProgressRepository;
import com.capstone.eqh.domain.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final UserMaterialProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final LessonEnrollmentRepository enrollmentRepository;
    private final MaterialPageUrlResolver pageUrlResolver;

    public List<MaterialListResponse> getMaterialsByLecture(Long lectureId, Long userId, Role role) {
        assertLectureExists(lectureId);
        if (role == Role.USER) {
            assertEnrolled(lectureId, userId);
        } else if (role == Role.PROF) {
            // 강의 목록은 수강 여부와 무관하게 조회 가능; 개별 material 접근 시 업로더 검증
        }

        return materialRepository.findByLesson_IdOrderByCreatedAtDesc(lectureId).stream()
                .filter(material -> canAccessMaterial(material, lectureId, userId, role))
                .map(this::toListResponse)
                .toList();
    }

    public MaterialDetailResponse getMaterialDetail(Long materialId, Long userId, Role role) {
        Material material = findMaterial(materialId);
        assertCanAccess(material, userId, role);
        return MaterialDetailResponse.from(material);
    }

    public ViewerResponse getViewer(Long materialId, Long userId, Role role) {
        Material material = findMaterial(materialId);
        assertCanAccess(material, userId, role);
        return ViewerResponse.from(material);
    }

    public PageResponse getPage(Long materialId, int pageNumber, Long userId, Role role) {
        Material material = findMaterial(materialId);
        assertCanAccess(material, userId, role);
        if (pageNumber < 1 || pageNumber > material.getPageCount()) {
            throw new IllegalArgumentException("유효하지 않은 페이지 번호입니다.");
        }
        return new PageResponse(pageNumber, pageUrlResolver.resolvePageImageUrl(material, pageNumber));
    }

    @Transactional
    public void saveProgress(Long materialId, ProgressRequest request, Long userId, Role role) {
        Material material = findMaterial(materialId);
        assertCanAccess(material, userId, role);

        int currentPage = request.currentPage();
        if (currentPage > material.getPageCount()) {
            throw new IllegalArgumentException("현재 페이지는 총 페이지 수를 초과할 수 없습니다.");
        }

        UserMaterialProgress progress = progressRepository
                .findByUserIdAndMaterialId(userId, materialId)
                .map(existing -> {
                    existing.updateCurrentPage(currentPage);
                    return existing;
                })
                .orElseGet(() -> UserMaterialProgress.builder()
                        .userId(userId)
                        .materialId(materialId)
                        .currentPage(currentPage)
                        .updatedAt(LocalDateTime.now())
                        .build());
        progressRepository.save(progress);
    }

    private MaterialListResponse toListResponse(Material material) {
        String thumbnail = material.getThumbnailUrl();
        if (thumbnail == null || thumbnail.isBlank()) {
            thumbnail = pageUrlResolver.resolvePageImageUrl(material, 1);
        }
        return MaterialListResponse.from(material, thumbnail);
    }

    public boolean isUploader(Long materialId, Long userId) {
        return materialRepository.findById(materialId)
                .map(m -> m.getUploadedBy() != null && m.getUploadedBy().getId().equals(userId))
                .orElse(false);
    }

    private Material findMaterial(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(MaterialNotFoundException::new);
    }

    private void assertLectureExists(Long lectureId) {
        if (!lessonRepository.existsById(lectureId)) {
            throw new MaterialNotFoundException();
        }
    }

    /**
     * 역할별 접근 정책:
     * - USER(학생): 승인된 수강 신청이 있는 강의의 교안만
     * - PROF(교수): 본인이 업로드한 교안만
     * - ADMIN: 전체 허용
     */
    private void assertCanAccess(Material material, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return;
        }
        if (role == Role.PROF) {
            if (!isUploader(material.getId(), userId)) {
                throw new MaterialAccessDeniedException();
            }
            return;
        }
        if (role == Role.USER) {
            assertEnrolled(material.getLectureId(), userId);
            return;
        }
        throw new MaterialAccessDeniedException();
    }

    private boolean canAccessMaterial(Material material, Long lectureId, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return true;
        }
        if (role == Role.PROF) {
            return isUploader(material.getId(), userId);
        }
        if (role == Role.USER) {
            return material.getLectureId().equals(lectureId)
                    && enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                    lectureId, userId, EnrollmentStatus.APPROVED);
        }
        return false;
    }

    private void assertEnrolled(Long lectureId, Long userId) {
        boolean approved = enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                lectureId, userId, EnrollmentStatus.APPROVED);
        if (!approved) {
            throw new MaterialAccessDeniedException();
        }
    }
}
