package com.capstone.eqh.domain.lesson.service;

import com.capstone.eqh.domain.lesson.dto.request.LessonCreateRequestDto;
import com.capstone.eqh.domain.lesson.dto.request.LessonUpdateRequestDto;
import com.capstone.eqh.domain.lesson.dto.response.LessonResponseDto;
import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.entity.LessonMaterial;
import com.capstone.eqh.domain.lesson.repository.LessonMaterialRepository;
import com.capstone.eqh.domain.lesson.repository.LessonRepository;
import com.capstone.eqh.domain.quiz.repository.QuizRepository;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMaterialRepository materialRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    @Transactional
    public LessonResponseDto create(LessonCreateRequestDto request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Lesson lesson = Lesson.builder()
                .title(request.title())
                .description(request.description())
                .createdBy(user)
                .build();
        return LessonResponseDto.from(lessonRepository.save(lesson));
    }

    public Page<LessonResponseDto> getAll(Long userId, Role role, Pageable pageable) {
        if (role == Role.PROF) {
            return lessonRepository.findAllByCreatedBy_Id(userId, pageable).map(LessonResponseDto::from);
        }
        return lessonRepository.findAll(pageable).map(LessonResponseDto::from);
    }

    public LessonResponseDto getOne(Long id) {
        return LessonResponseDto.from(findById(id));
    }

    @Transactional
    public LessonResponseDto update(Long id, LessonUpdateRequestDto request) {
        Lesson lesson = findById(id);
        lesson.update(request.title(), request.description());
        return LessonResponseDto.from(lesson);
    }

    @Transactional
    public void delete(Long id) {
        Lesson lesson = findById(id);
        List<LessonMaterial> materials = materialRepository.findAllByLessonId(id);
        for (LessonMaterial material : materials) {
            quizRepository.deleteAll(quizRepository.findAllByMaterial_Id(material.getId()));
        }
        lessonRepository.delete(lesson);
    }

    public boolean isOwner(Long lessonId, Long userId) {
        return lessonRepository.findById(lessonId)
                .map(l -> l.getCreatedBy() != null && l.getCreatedBy().getId().equals(userId))
                .orElse(false);
    }

    public Lesson findById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.LESSON_NOT_FOUND));
    }
}
