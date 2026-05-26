package com.capstone.eqh.global.jwt;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.global.security.CustomUserDetails;
import com.capstone.eqh.global.security.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    private JwtProvider jwtProvider;
    private CustomUserDetailsService userDetailsService;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        jwtFilter = new JwtFilter(jwtProvider, userDetailsService, new ObjectMapper());
    }

    @Test
    @DisplayName("/api/auth/** 경로는 무효 토큰이 헤더에 있어도 필터가 토큰 검증을 시도하지 않는다")
    void skipsValidation_onAuthPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/profsignup");
        request.setServletPath("/api/auth/profsignup");
        request.addHeader("Authorization", "Bearer invalid.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200); // 필터 단계에서는 차단 없음
        assertThat(chain.getRequest()).isSameAs(request); // 컨트롤러까지 전달
        verify(jwtProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("/oauth2/** 경로는 만료 토큰이 헤더에 있어도 필터가 토큰 검증을 시도하지 않는다")
    void skipsValidation_onOAuth2Path() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
        request.setServletPath("/oauth2/authorization/kakao");
        request.addHeader("Authorization", "Bearer expired.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(jwtProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("/login/oauth2/** 콜백 경로도 필터가 토큰 검증을 시도하지 않는다")
    void skipsValidation_onLoginOAuth2Path() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/kakao");
        request.setServletPath("/login/oauth2/code/kakao");
        request.addHeader("Authorization", "Bearer stale.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(jwtProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("/api/v1/auth/password/** 경로는 JWT 검증을 건너뛴다")
    void skipsValidation_onPasswordResetPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/auth/password/reset-request");
        request.setServletPath("/api/v1/auth/password/reset-request");
        request.addHeader("Authorization", "Bearer invalid.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(jwtProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("보호된 경로에 토큰이 있으면 필터가 검증을 수행한다")
    void validatesToken_onProtectedPath() throws ServletException, IOException {
        User user = User.builder()
                .username("홍길동")
                .nickname("gildong")
                .email("hong@test.com")
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        when(jwtProvider.getUserId("some.token.value")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("1")).thenReturn(new CustomUserDetails(user));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setServletPath("/api/users/me");
        request.addHeader("Authorization", "Bearer some.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        verify(jwtProvider, times(1)).validateToken("some.token.value");
    }
}
