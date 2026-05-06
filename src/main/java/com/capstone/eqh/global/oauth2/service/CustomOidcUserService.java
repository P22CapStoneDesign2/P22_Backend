package com.capstone.eqh.global.oauth2.service;

import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.AuthProvider;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.ErrorCode;
import com.capstone.eqh.global.oauth2.CustomOidcUser;
import com.capstone.eqh.global.oauth2.info.KakaoOAuth2UserInfo;
import com.capstone.eqh.global.oauth2.info.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = extractUserInfo(registrationId, oidcUser.getAttributes());
        AuthProvider provider = resolveProvider(registrationId);

        User user = findOrCreateUser(userInfo, provider);

        return new CustomOidcUser(oidcUser, Map.of(
                "dbUserId", user.getId(),
                "dbUserRole", user.getRole().name()
        ));
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return new KakaoOAuth2UserInfo(attributes);
        }
        throw toOAuth2Exception(ErrorCode.OAUTH2_UNSUPPORTED_PROVIDER);
    }

    private AuthProvider resolveProvider(String registrationId) {
        if ("kakao".equals(registrationId)) {
            return AuthProvider.KAKAO;
        }
        throw toOAuth2Exception(ErrorCode.OAUTH2_UNSUPPORTED_PROVIDER);
    }

    private User findOrCreateUser(OAuth2UserInfo userInfo, AuthProvider provider) {
        String providerId = userInfo.getId();

        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, providerId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String name = userInfo.getName();
        if (name == null || name.length() > 20) {
            name = "카카오유저";
        }

        String placeholderEmail = provider.name().toLowerCase() + "_" + providerId + "@social.user";

        User newUser = User.builder()
                .username(name)
                .email(placeholderEmail)
                .password(null)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .build();
        if (newUser == null) {
            throw toOAuth2Exception(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return userRepository.save(newUser);
    }

    private OAuth2AuthenticationException toOAuth2Exception(ErrorCode errorCode) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(errorCode.name(), errorCode.getMessage(), null),
                errorCode.getMessage()
        );
    }
}
