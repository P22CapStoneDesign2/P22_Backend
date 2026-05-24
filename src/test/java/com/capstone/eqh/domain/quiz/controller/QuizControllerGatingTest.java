package com.capstone.eqh.domain.quiz.controller;

import com.capstone.eqh.domain.quiz.dto.response.QuizDetailResponseDto;
import com.capstone.eqh.domain.quiz.dto.response.QuizResponseDto;
import com.capstone.eqh.domain.quiz.service.QuizService;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import com.capstone.eqh.global.config.JpaAuditingConfig;
import com.capstone.eqh.global.exception.GlobalExceptionHandler;
import com.capstone.eqh.global.jwt.JwtFilter;
import com.capstone.eqh.global.security.CustomUserDetails;
import com.capstone.eqh.global.security.SecurityConfig;
import com.capstone.eqh.support.WebMvcTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = QuizController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtFilter.class, JpaAuditingConfig.class}))
@Import({WebMvcTestSecurityConfig.class, GlobalExceptionHandler.class})
class QuizControllerGatingTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean QuizService quizService;

    private CustomUserDetails studentDetails;

    @BeforeEach
    void setUp() {
        User student = User.builder()
                .username("학생2")
                .nickname("nick2")
                .email("s2@test.com")
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(student, "id", 2L);
        studentDetails = new CustomUserDetails(student);
    }

    @Test
    @DisplayName("[게이팅 1] APPROVED 학생이 퀴즈 단건 조회 → 200")
    void getOne_approvedUser_returns200() throws Exception {
        QuizDetailResponseDto dto = new QuizDetailResponseDto(
                10L, "퀴즈", "설명", 3L, "교안", 1L, "교수", List.of(), null, null);
        when(quizService.getOne(eq(10L), eq(2L), eq(Role.USER))).thenReturn(dto);

        mockMvc.perform(get("/api/quiz/10").with(user(studentDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.materialId").value(3));
    }

    @Test
    @DisplayName("[게이팅 2] PENDING/미승인 학생이 퀴즈 조회 → 403 ENROLLMENT_NOT_APPROVED")
    void getOne_unapprovedUser_returns403() throws Exception {
        when(quizService.getOne(eq(10L), eq(2L), eq(Role.USER)))
                .thenThrow(new CustomException(ErrorCode.ENROLLMENT_NOT_APPROVED));

        mockMvc.perform(get("/api/quiz/10").with(user(studentDetails)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("수강 승인되지 않은 강의입니다."));
    }

    @Test
    @DisplayName("[게이팅 3] 미신청 학생이 퀴즈 제출 → 403 ENROLLMENT_NOT_APPROVED")
    void submit_unapprovedUser_returns403() throws Exception {
        when(quizService.submit(eq(10L), any(), eq(2L)))
                .thenThrow(new CustomException(ErrorCode.ENROLLMENT_NOT_APPROVED));

        String body = "{\"answers\":[{\"questionId\":100,\"studentAnswer\":\"답\"}]}";

        mockMvc.perform(post("/api/quiz/10/submit")
                        .with(user(studentDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("수강 승인되지 않은 강의입니다."));
    }

    @Test
    @DisplayName("[게이팅 4] USER GET /api/quiz: APPROVED 교안 있음 → 해당 퀴즈만 반환")
    void getAll_userScopedToApproved() throws Exception {
        QuizResponseDto dto = new QuizResponseDto(
                10L, "퀴즈", "설명", 3L, "교안", 1L, "교수", 0, null, null);
        when(quizService.getAll(eq(2L), eq(Role.USER), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/quiz").with(user(studentDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(10));
    }

    @Test
    @DisplayName("[게이팅 5] USER GET /api/quiz: APPROVED 교안 없음 → 빈 페이지")
    void getAll_userNoApproved_empty() throws Exception {
        when(quizService.getAll(eq(2L), eq(Role.USER), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/quiz").with(user(studentDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("[게이팅 6] USER GET /api/quiz?lessonId={approved} → 해당 교안 퀴즈 반환")
    void getAll_userWithApprovedLessonId() throws Exception {
        QuizResponseDto dto = new QuizResponseDto(
                10L, "퀴즈", "설명", 3L, "교안", 1L, "교수", 0, null, null);
        when(quizService.getAll(eq(2L), eq(Role.USER), eq(3L), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/quiz").param("materialId", "3").with(user(studentDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].materialId").value(3));
    }

    @Test
    @DisplayName("[게이팅 7] USER GET /api/quiz?lessonId={unapproved} → 빈 페이지")
    void getAll_userWithUnapprovedLessonId_empty() throws Exception {
        when(quizService.getAll(eq(2L), eq(Role.USER), eq(3L), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/quiz").param("materialId", "3").with(user(studentDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
}
