package com.capstone.eqh.domain.user.password.service;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.password.dto.request.PasswordResetConfirmRequestDto;
import com.capstone.eqh.domain.user.password.dto.response.PasswordResetResponse;
import com.capstone.eqh.domain.user.password.entity.PasswordResetToken;
import com.capstone.eqh.domain.user.password.exception.PasswordResetException;
import com.capstone.eqh.domain.user.password.exception.PasswordResetMailException;
import com.capstone.eqh.domain.user.password.redis.PasswordResetRedisStore;
import com.capstone.eqh.domain.user.password.repository.PasswordResetTokenRepository;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.config.PasswordResetProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordResetRedisStore redisStore;
    @Mock PasswordResetEmailService emailService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PasswordResetProperties properties;

    @InjectMocks
    PasswordResetService passwordResetService;

    private User localUser(Long id) {
        User user = User.builder()
                .username("테스트")
                .nickname("nick")
                .email("user@test.com")
                .password("encoded")
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("가입되지 않은 이메일은 메일 요청 거부")
    void requestReset_unknownEmail_throws() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.requestReset("unknown@test.com"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessage("가입되지 않은 이메일입니다.");

        verify(emailService, never()).sendPasswordResetLink(any(), any());
        verify(redisStore, never()).setCooldown(any());
    }

    @Test
    @DisplayName("소셜 계정 이메일은 재설정 불가")
    void validateEmail_socialAccount_throws() {
        User user = User.builder()
                .username("카카오")
                .nickname("kakao1")
                .email("kakao@test.com")
                .password(null)
                .provider(AuthProvider.KAKAO)
                .role(Role.USER)
                .build();
        when(userRepository.findByEmail("kakao@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> passwordResetService.validateEmailEligibility("kakao@test.com"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessageContaining("소셜");
    }

    @Test
    @DisplayName("가입된 LOCAL 이메일은 검증 통과")
    void validateEmail_localUser_success() {
        User user = localUser(1L);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        PasswordResetResponse response = passwordResetService.validateEmailEligibility("user@test.com");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).contains("가능");
    }

    @Test
    @DisplayName("가입된 LOCAL 사용자면 토큰 저장 후 메일 발송")
    void requestReset_localUser_sendsMail() {
        User user = localUser(1L);
        when(properties.getTokenTtlMinutes()).thenReturn(30);
        when(redisStore.getCooldownRemainingSeconds("user@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        PasswordResetResponse response = passwordResetService.requestReset("user@test.com");

        assertThat(response.success()).isTrue();
        verify(tokenRepository).expireActiveTokensByUserId(eq(1L), any(LocalDateTime.class));
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(redisStore).saveToken(anyString(), eq("user@test.com"));
        verify(emailService).sendPasswordResetLink(eq("user@test.com"), anyString());
    }

    @Test
    @DisplayName("쿨다운 중이면 재요청 거부")
    void requestReset_cooldown_throws() {
        User user = localUser(1L);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(redisStore.getCooldownRemainingSeconds("user@test.com")).thenReturn(Optional.of(30L));

        assertThatThrownBy(() -> passwordResetService.requestReset("user@test.com"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessageContaining("1분");
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void verifyToken_valid() {
        User user = localUser(1L);
        PasswordResetToken token = new PasswordResetToken(
                user, "abc", LocalDateTime.now().plusMinutes(30));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));
        when(redisStore.findEmailByToken("abc")).thenReturn(Optional.of("user@test.com"));

        PasswordResetResponse response = passwordResetService.verifyToken("abc");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).contains("유효");
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void verifyToken_expired() {
        User user = localUser(1L);
        PasswordResetToken token = new PasswordResetToken(
                user, "abc", LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.verifyToken("abc"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessage("만료된 토큰입니다.");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 실패")
    void resetPassword_mismatch() {
        var request = new PasswordResetConfirmRequestDto("t", "Pass123!", "Pass456!");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(PasswordResetException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 시 BCrypt 적용 및 used 처리")
    void resetPassword_success() {
        User user = localUser(1L);
        PasswordResetToken token = new PasswordResetToken(
                user, "tok", LocalDateTime.now().plusMinutes(30));
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
        when(redisStore.findEmailByToken("tok")).thenReturn(Optional.of("user@test.com"));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new-encoded");

        var request = new PasswordResetConfirmRequestDto("tok", "NewPass1!", "NewPass1!");
        PasswordResetResponse response = passwordResetService.resetPassword(request);

        assertThat(response.success()).isTrue();
        assertThat(token.isUsed()).isTrue();
        verify(passwordEncoder).encode("NewPass1!");
        verify(tokenRepository).expireActiveTokensByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("메일 발송 실패 시 예외 전파")
    void requestReset_mailFailure() {
        User user = localUser(1L);
        when(properties.getTokenTtlMinutes()).thenReturn(30);
        when(redisStore.getCooldownRemainingSeconds("user@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doThrow(new PasswordResetMailException("fail", new RuntimeException("smtp")))
                .when(emailService).sendPasswordResetLink(any(), any());

        assertThatThrownBy(() -> passwordResetService.requestReset("user@test.com"))
                .isInstanceOf(PasswordResetMailException.class);
    }
}
