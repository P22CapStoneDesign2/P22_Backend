package com.capstone.eqh.domain.quiz.controller;

import com.capstone.eqh.domain.quiz.service.QuizService;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.enums.UserStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = QuizController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtFilter.class, JpaAuditingConfig.class}))
@Import({WebMvcTestSecurityConfig.class, GlobalExceptionHandler.class})
class QuizControllerProfGatingTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean QuizService quizService;

    private CustomUserDetails pendingProfDetails;

    @BeforeEach
    void setUp() {
        User pending = User.builder()
                .username("승인대기교수")
                .nickname("pendprof")
                .email("pend@test.com")
                .password("encoded")
                .provider(AuthProvider.LOCAL)
                .role(Role.PROF)
                .status(UserStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(pending, "id", 10L);
        pendingProfDetails = new CustomUserDetails(pending);
    }

    @Test
    @DisplayName("[PROF 게이팅 3] PENDING PROF의 POST /api/quiz → 403 PROF_NOT_APPROVED")
    void createQuiz_pendingProf_returns403() throws Exception {
        String body = "{\"title\":\"새 퀴즈\",\"description\":\"설명\",\"lessonId\":1}";

        mockMvc.perform(post("/api/quiz")
                        .with(user(pendingProfDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("교수 계정 승인 대기 중입니다."));
    }
}
