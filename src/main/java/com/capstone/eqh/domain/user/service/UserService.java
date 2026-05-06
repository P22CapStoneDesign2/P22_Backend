package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.request.UserDeleteRequestDto;
import com.capstone.eqh.domain.user.dto.request.UserUpdateRequestDto;
import com.capstone.eqh.domain.user.dto.response.UserResponseDto;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.repository.RefreshTokenRepository;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto updateMyProfile(Long userId, UserUpdateRequestDto request) {
        User user = findActiveUser(userId);

        if (request.username() != null) {
            user.updateUsername(request.username());
        }

        if (request.newPassword() != null) {
            if (user.getProvider() != AuthProvider.LOCAL) {
                throw new CustomException(ErrorCode.SOCIAL_CANNOT_CHANGE_PASSWORD);
            }
            if (request.currentPassword() == null ||
                    !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.WRONG_CURRENT_PASSWORD);
            }
            user.updatePassword(passwordEncoder.encode(request.newPassword()));
        }

        return UserResponseDto.from(user);
    }

    @Transactional
    public void deleteMyAccount(Long userId, UserDeleteRequestDto request) {
        User user = findActiveUser(userId);

        if (user.getProvider() == AuthProvider.LOCAL) {
            if (request == null || request.password() == null ||
                    !passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new CustomException(ErrorCode.WRONG_PASSWORD);
            }
        }

        user.softDelete();
        refreshTokenRepository.deleteByUserId(userId);
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
