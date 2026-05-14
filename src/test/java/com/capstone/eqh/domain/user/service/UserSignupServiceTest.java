package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.request.ProfSignupRequestDto;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSignupServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserSignupService userSignupService;

    private ProfSignupRequestDto profRequest(String password, String passwordConfirm) {
        return new ProfSignupRequestDto(
                "홍길동",
                "hong@university.ac.kr",
                "gildong",
                password,
                passwordConfirm);
    }

    @Test
    @DisplayName("profSignup 성공: Role.PROF + AuthProvider.LOCAL로 저장")
    void profSignup_success() {
        ProfSignupRequestDto request = profRequest("Password1!", "Password1!");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");

        userSignupService.profSignup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.PROF);
        assertThat(saved.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getEmail()).isEqualTo("hong@university.ac.kr");
        assertThat(saved.getNickname()).isEqualTo("gildong");
        assertThat(saved.getUsername()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("profSignup 실패: password != passwordConfirm 이면 PASSWORD_CONFIRM_MISMATCH")
    void profSignup_passwordMismatch() {
        ProfSignupRequestDto request = profRequest("Password1!", "Different1!");

        assertThatThrownBy(() -> userSignupService.profSignup(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("profSignup 실패: 이메일 중복이면 EMAIL_ALREADY_EXISTS")
    void profSignup_emailDuplicate() {
        ProfSignupRequestDto request = profRequest("Password1!", "Password1!");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userSignupService.profSignup(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("profSignup 실패: 닉네임 중복이면 NICKNAME_ALREADY_EXISTS")
    void profSignup_nicknameDuplicate() {
        ProfSignupRequestDto request = profRequest("Password1!", "Password1!");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByNickname(request.nickname())).thenReturn(true);

        assertThatThrownBy(() -> userSignupService.profSignup(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeSocialSignup 성공: Role.USER + KAKAO provider/providerId, password=null")
    void completeSocialSignup_success() {
        String providerId = "kakao-123";
        when(userRepository.existsByEmail("student@gmail.com")).thenReturn(false);
        when(userRepository.existsByNickname("studyking")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userSignupService.completeSocialSignup(
                providerId, AuthProvider.KAKAO,
                "김학생", "student@gmail.com", "studyking");

        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(saved.getProviderId()).isEqualTo(providerId);
        assertThat(saved.getPassword()).isNull();
        assertThat(saved.getUsername()).isEqualTo("김학생");
        assertThat(saved.getEmail()).isEqualTo("student@gmail.com");
        assertThat(saved.getNickname()).isEqualTo("studyking");
    }

    @Test
    @DisplayName("completeSocialSignup 실패: 이메일 중복이면 EMAIL_ALREADY_EXISTS")
    void completeSocialSignup_emailDuplicate() {
        when(userRepository.existsByEmail("student@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> userSignupService.completeSocialSignup(
                "kakao-123", AuthProvider.KAKAO, "김학생", "student@gmail.com", "studyking"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeSocialSignup 실패: 닉네임 중복이면 NICKNAME_ALREADY_EXISTS")
    void completeSocialSignup_nicknameDuplicate() {
        when(userRepository.existsByEmail("student@gmail.com")).thenReturn(false);
        when(userRepository.existsByNickname("studyking")).thenReturn(true);

        assertThatThrownBy(() -> userSignupService.completeSocialSignup(
                "kakao-123", AuthProvider.KAKAO, "김학생", "student@gmail.com", "studyking"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("isNicknameAvailable: 미사용이면 true, 사용 중이면 false")
    void isNicknameAvailable_returnsBasedOnExists() {
        when(userRepository.existsByNickname("free")).thenReturn(false);
        when(userRepository.existsByNickname("taken")).thenReturn(true);

        assertThat(userSignupService.isNicknameAvailable("free")).isTrue();
        assertThat(userSignupService.isNicknameAvailable("taken")).isFalse();
    }
}
