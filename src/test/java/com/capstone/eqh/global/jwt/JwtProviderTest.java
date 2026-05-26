package com.capstone.eqh.global.jwt;

import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "eznHPpYI/9FkNLt1ngJQTxaFfjePbMjqghP2e2fVrUE=";
    private static final String OTHER_SECRET = "K7p2HpV2vGqOQy0u5GxFp1XdPgRkKkqLpwvY5jH2MXY=";

    private JwtProvider jwtProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET);
        secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    @Test
    @DisplayName("generatePendingToken: sub=providerId, type=PENDING_SOCIAL, provider/name 클레임 포함")
    void generatePendingToken_containsExpectedClaims() {
        String token = jwtProvider.generatePendingToken("kakao-123", "KAKAO", "홍길동");

        var claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("kakao-123");
        assertThat(claims.get("type", String.class)).isEqualTo("PENDING_SOCIAL");
        assertThat(claims.get("provider", String.class)).isEqualTo("KAKAO");
        assertThat(claims.get("name", String.class)).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("getPendingTokenClaims 성공: providerId/provider/name 맵 반환")
    void getPendingTokenClaims_success() {
        String token = jwtProvider.generatePendingToken("kakao-123", "KAKAO", "홍길동");

        Map<String, String> claims = jwtProvider.getPendingTokenClaims(token);

        assertThat(claims).containsEntry("providerId", "kakao-123");
        assertThat(claims).containsEntry("provider", "KAKAO");
        assertThat(claims).containsEntry("name", "홍길동");
    }

    @Test
    @DisplayName("getPendingTokenClaims 실패: access 토큰을 넘기면 INVALID_PENDING_TOKEN (type 불일치)")
    void getPendingTokenClaims_typeMismatch() {
        String accessToken = jwtProvider.generateAccessToken(1L, "USER");

        assertThatThrownBy(() -> jwtProvider.getPendingTokenClaims(accessToken))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PENDING_TOKEN);
    }

    @Test
    @DisplayName("getPendingTokenClaims 실패: 다른 키로 서명된 토큰이면 INVALID_PENDING_TOKEN")
    void getPendingTokenClaims_tampered() {
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(OTHER_SECRET));
        Date now = new Date();
        String foreignToken = Jwts.builder()
                .subject("kakao-123")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .claim("type", "PENDING_SOCIAL")
                .claim("provider", "KAKAO")
                .claim("name", "홍길동")
                .signWith(otherKey)
                .compact();

        assertThatThrownBy(() -> jwtProvider.getPendingTokenClaims(foreignToken))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PENDING_TOKEN);
    }

    @Test
    @DisplayName("getPendingTokenClaims 실패: 만료된 pending 토큰이면 INVALID_PENDING_TOKEN")
    void getPendingTokenClaims_expired() {
        Date past = new Date(System.currentTimeMillis() - 60_000);
        Date expired = new Date(System.currentTimeMillis() - 1_000);
        String expiredToken = Jwts.builder()
                .subject("kakao-123")
                .issuedAt(past)
                .expiration(expired)
                .claim("type", "PENDING_SOCIAL")
                .claim("provider", "KAKAO")
                .claim("name", "홍길동")
                .signWith(secretKey)
                .compact();

        assertThatThrownBy(() -> jwtProvider.getPendingTokenClaims(expiredToken))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PENDING_TOKEN);
    }
}
