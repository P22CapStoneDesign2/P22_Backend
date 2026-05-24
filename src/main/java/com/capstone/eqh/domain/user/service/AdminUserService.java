package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.response.PendingUserResponseDto;
import com.capstone.eqh.domain.user.dto.response.UserStatusResponseDto;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.enums.UserStatus;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public Page<PendingUserResponseDto> listPending(Pageable pageable) {
        return userRepository.findByRoleAndStatus(Role.PROF, UserStatus.PENDING, pageable)
                .map(PendingUserResponseDto::from);
    }

    @Transactional
    public UserStatusResponseDto approve(Long userId) {
        User user = findUser(userId);
        user.updateStatus(UserStatus.ACTIVE);
        return UserStatusResponseDto.from(user);
    }

    @Transactional
    public UserStatusResponseDto reject(Long userId) {
        User user = findUser(userId);
        user.updateStatus(UserStatus.REJECTED);
        return UserStatusResponseDto.from(user);
    }

    @Transactional
    public UserStatusResponseDto changeStatus(Long userId, UserStatus status) {
        User user = findUser(userId);
        user.updateStatus(status);
        return UserStatusResponseDto.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
