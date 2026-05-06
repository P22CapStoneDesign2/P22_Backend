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

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        if (user == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        userRepository.save(user);
    }
}
