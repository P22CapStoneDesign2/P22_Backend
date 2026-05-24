package com.capstone.eqh.domain.lesson.controller;

import com.capstone.eqh.domain.lesson.dto.response.LessonResponseDto;
import com.capstone.eqh.domain.lesson.service.LessonService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = LessonController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtFilter.class, JpaAuditingConfig.class}))
@Import({WebMvcTestSecurityConfig.class, GlobalExceptionHandler.class})
class LessonControllerProfGatingTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LessonService lessonService;

    private CustomUserDetails pendingProfDetails;
    private CustomUserDetails activeProfDetails;

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

        User active = User.builder()
                .username("활성교수")
                .nickname("activeprof")
                .email("active@test.com")
                .password("encoded")
                .provider(AuthProvider.LOCAL)
                .role(Role.PROF)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(active, "id", 11L);
        activeProfDetails = new CustomUserDetails(active);
    }

    @Test
    @DisplayName("[PROF 게이팅 1] PENDING PROF의 POST /api/lessons → 403 PROF_NOT_APPROVED")
    void create_pendingProf_returns403() throws Exception {
        String body = "{\"title\":\"새 교안\",\"description\":\"설명\"}";

        mockMvc.perform(post("/api/lessons")
                        .with(user(pendingProfDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("교수 계정 승인 대기 중입니다."));
    }

    @Test
    @DisplayName("[PROF 게이팅 2] ACTIVE PROF의 POST /api/lessons → 201")
    void create_activeProf_returns201() throws Exception {
        LessonResponseDto dto = new LessonResponseDto(
                1L, "새 교안", "설명", 11L, "활성교수",
                LocalDateTime.now(), LocalDateTime.now());
        when(lessonService.create(any(), eq(11L))).thenReturn(dto);

        String body = "{\"title\":\"새 교안\",\"description\":\"설명\"}";

        mockMvc.perform(post("/api/lessons")
                        .with(user(activeProfDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("새 교안"));
    }
}
