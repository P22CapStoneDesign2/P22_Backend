package com.capstone.eqh.domain.lesson.service;

import com.capstone.eqh.domain.lesson.dto.request.LessonCreateRequestDto;
import com.capstone.eqh.domain.lesson.dto.request.LessonUpdateRequestDto;
import com.capstone.eqh.domain.lesson.dto.response.LessonResponseDto;
import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.lesson.repository.LessonRepository;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock UserRepository userRepository;
    @InjectMocks LessonService lessonService;

    private User createUser(Long id) {
        User user = User.builder()
                .username("교수")
                .nickname("prof" + id)
                .email("prof" + id + "@test.com")
                .provider(AuthProvider.LOCAL)
                .role(Role.PROF)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Lesson createLesson(Long id, User creator) {
        Lesson lesson = Lesson.builder()
                .title("교안")
                .description("설명")
                .createdBy(creator)
                .build();
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }

    @Test
    @DisplayName("create 성공: 교안 저장 후 응답 DTO 반환")
    void create_success() {
        LessonCreateRequestDto request = new LessonCreateRequestDto("교안", "설명");
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson l = invocation.getArgument(0);
            ReflectionTestUtils.setField(l, "id", 10L);
            return l;
        });

        LessonResponseDto result = lessonService.create(request, 1L);

        assertThat(result.title()).isEqualTo("교안");
        assertThat(result.description()).isEqualTo("설명");
        assertThat(result.createdById()).isEqualTo(1L);
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("create 실패: 사용자가 존재하지 않으면 USER_NOT_FOUND")
    void create_userNotFound() {
        LessonCreateRequestDto request = new LessonCreateRequestDto("교안", "설명");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.create(request, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getOne 성공: 교안 단건 조회")
    void getOne_success() {
        Lesson lesson = createLesson(10L, createUser(1L));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        LessonResponseDto result = lessonService.getOne(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("교안");
    }

    @Test
    @DisplayName("update 성공: 제목·설명 갱신 후 응답 반환")
    void update_success() {
        Lesson lesson = createLesson(10L, createUser(1L));
        LessonUpdateRequestDto request = new LessonUpdateRequestDto("새 제목", "새 설명");
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        LessonResponseDto result = lessonService.update(10L, request);

        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.description()).isEqualTo("새 설명");
    }

    @Test
    @DisplayName("update 실패: 교안이 없으면 LESSON_NOT_FOUND")
    void update_notFound() {
        LessonUpdateRequestDto request = new LessonUpdateRequestDto("새 제목", "새 설명");
        when(lessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.update(99L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LESSON_NOT_FOUND);
    }

    @Test
    @DisplayName("delete 실패: 교안이 없으면 LESSON_NOT_FOUND")
    void delete_notFound() {
        when(lessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.delete(99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LESSON_NOT_FOUND);
    }

    @Test
    @DisplayName("isOwner: 생성자 ID와 일치하면 true")
    void isOwner_true() {
        User creator = createUser(1L);
        Lesson lesson = createLesson(10L, creator);
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        assertThat(lessonService.isOwner(10L, 1L)).isTrue();
    }

    @Test
    @DisplayName("isOwner: 생성자 ID와 다르면 false")
    void isOwner_notOwner() {
        User creator = createUser(1L);
        Lesson lesson = createLesson(10L, creator);
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        assertThat(lessonService.isOwner(10L, 999L)).isFalse();
    }

    @Test
    @DisplayName("isOwner: 교안이 없으면 false")
    void isOwner_lessonNotFound() {
        when(lessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(lessonService.isOwner(99L, 1L)).isFalse();
    }

    @Test
    @DisplayName("isOwner: createdBy가 null이면 false")
    void isOwner_creatorNull() {
        Lesson lesson = createLesson(10L, null);
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        assertThat(lessonService.isOwner(10L, 1L)).isFalse();
    }
}
