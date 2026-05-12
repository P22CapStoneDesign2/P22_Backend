package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.request.SignupRequestDto;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequestDto request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.username())
                .nickname(request.nickname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public User findOrCreateSocialUser(String providerId, String name, AuthProvider provider) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createSocialUser(providerId, name, provider));
    }

    private User createSocialUser(String providerId, String name, AuthProvider provider) {
        String resolvedName = (name == null || name.length() > 20) ? "카카오유저" : name;
        String placeholderEmail = provider.name().toLowerCase() + "_" + providerId + "@social.user";
        String nickname = generateUniqueNickname(providerId);

        return userRepository.save(User.builder()
                .username(resolvedName)
                .nickname(nickname)
                .email(placeholderEmail)
                .password(null)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .build());
    }

    private String generateUniqueNickname(String providerId) {
        String suffix = providerId.length() >= 6
                ? providerId.substring(providerId.length() - 6)
                : providerId;
        String candidate = "k" + suffix;
        if (!userRepository.existsByNickname(candidate)) {
            return candidate;
        }
        candidate = "k" + (providerId.length() >= 7
                ? providerId.substring(providerId.length() - 7)
                : providerId);
        if (candidate.length() <= 8 && !userRepository.existsByNickname(candidate)) {
            return candidate;
        }
        throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }
}
