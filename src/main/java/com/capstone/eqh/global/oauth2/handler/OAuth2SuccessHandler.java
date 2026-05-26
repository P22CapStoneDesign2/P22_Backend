package com.capstone.eqh.global.oauth2.handler;

import com.capstone.eqh.domain.user.service.UserAuthService;
import com.capstone.eqh.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserAuthService userAuthService;
    private final JwtProvider jwtProvider;

    @Value("${app.oauth2.redirect-uri:http://localhost:5174/oauth2/callback}")
    private String redirectUri;

    @Value("${app.oauth2.register-uri:http://localhost:5174/oauth2/register}")
    private String registerUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        boolean isNewUser = Boolean.TRUE.equals(oAuth2User.getAttributes().get("isNewUser"));

        if (isNewUser) {
            redirectNewUser(request, response, oAuth2User);
        } else {
            redirectExistingUser(request, response, oAuth2User);
        }
    }

    private void redirectNewUser(HttpServletRequest request,
                                  HttpServletResponse response,
                                  OAuth2User oAuth2User) throws IOException {
        String providerId   = (String) oAuth2User.getAttributes().get("providerId");
        String providerName = (String) oAuth2User.getAttributes().get("providerName");
        String kakaoName    = (String) oAuth2User.getAttributes().get("kakaoName");

        String pendingToken = jwtProvider.generatePendingToken(providerId, providerName, kakaoName);

        String url = UriComponentsBuilder.fromUriString(registerUri)
                .queryParam("pendingToken", pendingToken)
                .queryParam("kakaoName", URLEncoder.encode(kakaoName, StandardCharsets.UTF_8))
                .build(true)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, url);
    }

    private void redirectExistingUser(HttpServletRequest request,
                                       HttpServletResponse response,
                                       OAuth2User oAuth2User) throws IOException {
        Long userId = ((Number) oAuth2User.getAttributes().get("dbUserId")).longValue();
        String role = (String) oAuth2User.getAttributes().get("dbUserRole");

        String[] tokens = userAuthService.issueTokenPair(userId, role);

        String url = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokens[0])
                .queryParam("refreshToken", tokens[1])
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, url);
    }
}
