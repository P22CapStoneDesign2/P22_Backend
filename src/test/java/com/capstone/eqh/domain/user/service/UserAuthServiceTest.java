package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.request.LoginRequestDto;
import com.capstone.eqh.domain.user.dto.request.LogoutRequestDto;
import com.capstone.eqh.domain.user.dto.request.ReissueRequestDto;
import com.capstone.eqh.domain.user.dto.request.UserSocialSignupRequestDto;
import com.capstone.eqh.domain.user.dto.response.AuthResponseDto;
import com.capstone.eqh.domain.user.entity.RefreshToken;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.RefreshTokenRepository;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import com.capstone.eqh.global.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;
    @Mock UserSignupService userSignupService;
    @InjectMocks UserAuthService userAuthService;

    private User createUser(Long id, AuthProvider provider) {
        User user = User.builder()
                .username("테스트")
                .nickname("test" + id)
                .email("user" + id + "@test.com")
                .password("encoded")
                .provider(provider)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("login 성공: 유효한 자격증명이면 토큰 페어 발급")
    void login_success() {
        LoginRequestDto request = new LoginRequestDto("user1@test.com", "password");
        User user = createUser(1L, AuthProvider.LOCAL);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(jwtProvider.generateAccessToken(1L, "USER")).thenReturn("access");
        when(jwtProvider.generateRefreshToken(1L)).thenReturn("refresh");
        when(jwtProvider.getExpiration("refresh"))
                .thenReturn(new Date(System.currentTimeMillis() + 100_000));
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

        AuthResponseDto result = userAuthService.login(request);

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login 실패: 존재하지 않는 이메일이면 USER_NOT_FOUND")
    void login_userNotFound() {
        LoginRequestDto request = new LoginRequestDto("none@test.com", "password");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAuthService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("login 실패: 탈퇴(soft-delete)된 유저면 USER_NOT_FOUND")
    void login_softDeletedUser() {
        LoginRequestDto request = new LoginRequestDto("user1@test.com", "password");
        User user = createUser(1L, AuthProvider.LOCAL);
        user.softDelete();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAuthService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("login 실패: KAKAO 계정으로 일반 로그인 시도하면 SOCIAL_ACCOUNT_CONFLICT")
    void login_socialAccount() {
        LoginRequestDto request = new LoginRequestDto("user1@test.com", "password");
        User user = createUser(1L, AuthProvider.KAKAO);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAuthService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_CONFLICT);
    }

    @Test
    @DisplayName("login 실패: 비밀번호 불일치 시 INVALID_CREDENTIALS")
    void login_wrongPassword() {
        LoginRequestDto request = new LoginRequestDto("user1@test.com", "wrong");
        User user = createUser(1L, AuthProvider.LOCAL);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> userAuthService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("reissue 실패: 토큰에서 userId 추출 불가 시 INVALID_TOKEN")
    void reissue_invalidToken() {
        ReissueRequestDto request = new ReissueRequestDto("invalid");
        when(jwtProvider.getUserIdIgnoringExpiry("invalid")).thenReturn(null);

        assertThatThrownBy(() -> userAuthService.reissue(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("reissue 실패: DB에 저장된 토큰이 없으면 INVALID_TOKEN")
    void reissue_tokenNotInDb() {
        ReissueRequestDto request = new ReissueRequestDto("token");
        when(jwtProvider.getUserIdIgnoringExpiry("token")).thenReturn(1L);
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAuthService.reissue(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("reissue 실패: 만료된 토큰이면 토큰 삭제 후 EXPIRED_TOKEN")
    void reissue_expiredToken() {
        ReissueRequestDto request = new ReissueRequestDto("token");
        RefreshToken stored = RefreshToken.builder()
                .token("token")
                .userId(1L)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();

        when(jwtProvider.getUserIdIgnoringExpiry("token")).thenReturn(1L);
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> userAuthService.reissue(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPIRED_TOKEN);
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    @DisplayName("reissue 성공: 새 토큰 발급 및 기존 토큰 갱신")
    void reissue_success() {
        ReissueRequestDto request = new ReissueRequestDto("oldToken");
        User user = createUser(1L, AuthProvider.LOCAL);
        RefreshToken stored = RefreshToken.builder()
                .token("oldToken")
                .userId(1L)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();

        when(jwtProvider.getUserIdIgnoringExpiry("oldToken")).thenReturn(1L);
        when(refreshTokenRepository.findByToken("oldToken")).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtProvider.generateAccessToken(1L, "USER")).thenReturn("newAccess");
        when(jwtProvider.generateRefreshToken(1L)).thenReturn("newRefresh");
        when(jwtProvider.getExpiration("newRefresh"))
                .thenReturn(new Date(System.currentTimeMillis() + 100_000));

        AuthResponseDto result = userAuthService.reissue(request);

        assertThat(result.accessToken()).isEqualTo("newAccess");
        assertThat(result.refreshToken()).isEqualTo("newRefresh");
        assertThat(stored.getToken()).isEqualTo("newRefresh");
    }

    @Test
    @DisplayName("logout: 토큰이 존재하면 삭제")
    void logout_deletesToken() {
        LogoutRequestDto request = new LogoutRequestDto("token");
        RefreshToken stored = RefreshToken.builder()
                .token("token")
                .userId(1L)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(stored));

        userAuthService.logout(request);

        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    @DisplayName("logout: 토큰이 없으면 아무것도 하지 않음")
    void logout_noOpIfNotFound() {
        LogoutRequestDto request = new LogoutRequestDto("token");
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.empty());

        userAuthService.logout(request);

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("completeSocialSignup 성공: UserSignupService 위임 후 JWT 발급 + RefreshToken 저장")
    void completeSocialSignup_success() {
        UserSocialSignupRequestDto request = new UserSocialSignupRequestDto(
                "pending-token", "김학생", "student@gmail.com", "studyking");
        User user = createUser(7L, AuthProvider.KAKAO);

        when(jwtProvider.getPendingTokenClaims("pending-token")).thenReturn(Map.of(
                "providerId", "kakao-123",
                "provider", "KAKAO",
                "name", "기존이름"));
        when(userSignupService.completeSocialSignup(
                "kakao-123", AuthProvider.KAKAO,
                "김학생", "student@gmail.com", "studyking")).thenReturn(user);
        when(jwtProvider.generateAccessToken(7L, "USER")).thenReturn("access");
        when(jwtProvider.generateRefreshToken(7L)).thenReturn("refresh");
        when(jwtProvider.getExpiration("refresh"))
                .thenReturn(new Date(System.currentTimeMillis() + 100_000));
        when(refreshTokenRepository.findByUserId(7L)).thenReturn(Optional.empty());

        AuthResponseDto result = userAuthService.completeSocialSignup(request);

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("completeSocialSignup 실패: pending 토큰이 INVALID_PENDING_TOKEN이면 UserSignupService 미호출")
    void completeSocialSignup_invalidPendingToken() {
        UserSocialSignupRequestDto request = new UserSocialSignupRequestDto(
                "bad-token", "김학생", "student@gmail.com", "studyking");
        when(jwtProvider.getPendingTokenClaims("bad-token"))
                .thenThrow(new CustomException(ErrorCode.INVALID_PENDING_TOKEN));

        assertThatThrownBy(() -> userAuthService.completeSocialSignup(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PENDING_TOKEN);
        verifyNoInteractions(userSignupService);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
