package com.capstone.eqh.domain.lesson.service;

import com.capstone.eqh.domain.lesson.dto.request.LessonCreateRequestDto;
import com.capstone.eqh.domain.lesson.dto.request.LessonUpdateRequestDto;
import com.capstone.eqh.domain.lesson.dto.response.LessonMaterialResponseDto;
import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.entity.LessonEnrollment;
import com.capstone.eqh.domain.lesson.entity.LessonMaterial;
import com.capstone.eqh.domain.lesson.enums.EnrollmentStatus;
import com.capstone.eqh.domain.lesson.repository.LessonEnrollmentRepository;
import com.capstone.eqh.domain.lesson.repository.LessonMaterialRepository;
import com.capstone.eqh.domain.quiz.repository.QuizRepository;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonMaterialService {

    private final LessonMaterialRepository materialRepository;
    private final LessonEnrollmentRepository enrollmentRepository;
    private final LessonService lessonService;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;

    @Transactional
    public LessonMaterialResponseDto create(Long lessonId, LessonCreateRequestDto request, Long userId) {
        Lesson lesson = lessonService.findById(lessonId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        LessonMaterial material = LessonMaterial.builder()
                .lesson(lesson)
                .title(request.title())
                .description(request.description())
                .createdBy(user)
                .build();
        return LessonMaterialResponseDto.from(materialRepository.save(material));
    }

    public Page<LessonMaterialResponseDto> getAll(Long lessonId, Long userId, Role role, Pageable pageable) {
        lessonService.findById(lessonId);
        if (role == Role.USER) {
            boolean approved = enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                    lessonId, userId, EnrollmentStatus.APPROVED);
            if (!approved) {
                throw new CustomException(ErrorCode.LESSON_MATERIAL_ACCESS_DENIED);
            }
        }
        return materialRepository.findAllByLessonId(lessonId, pageable)
                .map(LessonMaterialResponseDto::from);
    }

    public LessonMaterialResponseDto getOne(Long lessonId, Long materialId, Long userId, Role role) {
        LessonMaterial material = findMaterialInLesson(lessonId, materialId);
        if (role == Role.USER) {
            boolean approved = enrollmentRepository.existsByLessonIdAndStudentIdAndStatus(
                    lessonId, userId, EnrollmentStatus.APPROVED);
            if (!approved) {
                throw new CustomException(ErrorCode.LESSON_MATERIAL_ACCESS_DENIED);
            }
        }
        return LessonMaterialResponseDto.from(material);
    }

    @Transactional
    public LessonMaterialResponseDto update(Long lessonId, Long materialId, LessonUpdateRequestDto request) {
        LessonMaterial material = findMaterialInLesson(lessonId, materialId);
        material.update(request.title(), request.description());
        return LessonMaterialResponseDto.from(material);
    }

    @Transactional
    public void delete(Long lessonId, Long materialId) {
        LessonMaterial material = findMaterialInLesson(lessonId, materialId);
        quizRepository.deleteAll(quizRepository.findAllByMaterial_Id(materialId));
        materialRepository.delete(material);
    }

    public boolean isOwner(Long materialId, Long userId) {
        return materialRepository.findById(materialId)
                .map(m -> m.getCreatedBy() != null && m.getCreatedBy().getId().equals(userId))
                .orElse(false);
    }

    public LessonMaterial findById(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new CustomException(ErrorCode.LESSON_MATERIAL_NOT_FOUND));
    }

    private LessonMaterial findMaterialInLesson(Long lessonId, Long materialId) {
        LessonMaterial material = findById(materialId);
        if (!material.getLesson().getId().equals(lessonId)) {
            throw new CustomException(ErrorCode.LESSON_MATERIAL_NOT_IN_LESSON);
        }
        return material;
    }
}
