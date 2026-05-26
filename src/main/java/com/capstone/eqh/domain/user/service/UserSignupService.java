package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.request.ProfSignupRequestDto;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.enums.UserStatus;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public User profSignup(ProfSignupRequestDto request) {
        emailVerificationService.requireEmailVerifiedForSignup(request.email());

        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            if (existing.getStatus() == UserStatus.REJECTED) {
                throw new CustomException(ErrorCode.EMAIL_REJECTED);
            }
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        });

        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = userRepository.save(User.builder()
                .username(request.username())
                .email(request.email())
                .nickname(request.nickname())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .role(Role.PROF)
                .status(UserStatus.PENDING)
                .build());

        emailVerificationService.consumeEmailVerification(request.email());
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> findSocialUser(String providerId, AuthProvider provider) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    @Transactional
    public User completeSocialSignup(String providerId, AuthProvider provider,
                                     String username, String email, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .nickname(nickname)
                .password(null)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }
}
