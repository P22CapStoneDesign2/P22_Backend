package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.response.PendingUserResponseDto;
import com.capstone.eqh.domain.user.dto.response.UserStatusResponseDto;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.enums.UserStatus;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks AdminUserService adminUserService;

    private User profUser(Long id, UserStatus status) {
        User user = User.builder()
                .username("교수" + id)
                .nickname("prof" + id)
                .email("prof" + id + "@test.com")
                .password("encoded")
                .provider(AuthProvider.LOCAL)
                .role(Role.PROF)
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("approve: PENDING PROF를 ACTIVE로 변경")
    void approve_pendingProf() {
        User pending = profUser(5L, UserStatus.PENDING);
        when(userRepository.findById(5L)).thenReturn(Optional.of(pending));

        UserStatusResponseDto result = adminUserService.approve(5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(pending.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("reject: PENDING PROF를 REJECTED로 변경")
    void reject_pendingProf() {
        User pending = profUser(7L, UserStatus.PENDING);
        when(userRepository.findById(7L)).thenReturn(Optional.of(pending));

        UserStatusResponseDto result = adminUserService.reject(7L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo(UserStatus.REJECTED);
        assertThat(pending.getStatus()).isEqualTo(UserStatus.REJECTED);
    }

    @Test
    @DisplayName("changeStatus: REJECTED를 ACTIVE로 변경 가능")
    void changeStatus_rejectedToActive() {
        User rejected = profUser(9L, UserStatus.REJECTED);
        when(userRepository.findById(9L)).thenReturn(Optional.of(rejected));

        UserStatusResponseDto result = adminUserService.changeStatus(9L, UserStatus.ACTIVE);

        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(rejected.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("listPending: PENDING + PROF 페이지를 PendingUserResponseDto로 매핑")
    void listPending_returnsPendingProfs() {
        Pageable pageable = PageRequest.of(0, 10);
        User pending1 = profUser(1L, UserStatus.PENDING);
        User pending2 = profUser(2L, UserStatus.PENDING);
        Page<User> page = new PageImpl<>(List.of(pending1, pending2), pageable, 2);
        when(userRepository.findByRoleAndStatus(Role.PROF, UserStatus.PENDING, pageable))
                .thenReturn(page);

        Page<PendingUserResponseDto> result = adminUserService.listPending(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(PendingUserResponseDto::id)
                .containsExactly(1L, 2L);
        assertThat(result.getContent()).allMatch(dto -> dto.status() == UserStatus.PENDING);
    }

    @Test
    @DisplayName("approve 실패: 존재하지 않는 사용자면 USER_NOT_FOUND")
    void approve_userNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.approve(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
