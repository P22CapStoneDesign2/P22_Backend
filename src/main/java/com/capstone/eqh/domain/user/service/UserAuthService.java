package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.request.LoginRequestDto;
import com.capstone.eqh.domain.user.dto.request.LogoutRequestDto;
import com.capstone.eqh.domain.user.dto.request.ReissueRequestDto;
import com.capstone.eqh.domain.user.dto.response.AuthResponseDto;
import com.capstone.eqh.domain.user.entity.RefreshToken;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.repository.RefreshTokenRepository;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import com.capstone.eqh.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new CustomException(ErrorCode.SOCIAL_ACCOUNT_CONFLICT);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        saveOrUpdateRefreshToken(user.getId(), refreshToken);

        return new AuthResponseDto(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponseDto reissue(ReissueRequestDto request) {
        String oldToken = request.refreshToken();

        Long userId = jwtProvider.getUserIdIgnoringExpiry(oldToken);
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        RefreshToken stored = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId());

        stored.updateToken(newRefreshToken, toLocalDateTime(jwtProvider.getExpiration(newRefreshToken)));

        return new AuthResponseDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public String[] issueTokenPair(Long userId, String role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        saveOrUpdateRefreshToken(userId, refreshToken);
        return new String[]{accessToken, refreshToken};
    }

    @Transactional
    public void logout(LogoutRequestDto request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

    private void saveOrUpdateRefreshToken(Long userId, String token) {
        LocalDateTime expiryDate = toLocalDateTime(jwtProvider.getExpiration(token));

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.updateToken(token, expiryDate),
                        () -> {
                            RefreshToken newToken = RefreshToken.builder()
                                    .token(token)
                                    .userId(userId)
                                    .expiryDate(expiryDate)
                                    .build();
                            if (newToken == null) {
                                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
                            }
                            refreshTokenRepository.save(newToken);
                        }
                );
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
