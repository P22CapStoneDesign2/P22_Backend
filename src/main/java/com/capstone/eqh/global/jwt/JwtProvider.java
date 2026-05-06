/**
 * jjwt 0.12.x API 사용 (Jwts.builder(), Jwts.parser(), .verifyWith(), .parseSignedClaims())
 * @Value("${jwt.secret}") 주입 → application.yml에서 Base64 인코딩된 256비트 이상 키 필요
 * validateToken()은 필터에서, isTokenExpired()는 reissue 로직에서 분리 사용
 * generateRefreshToken()에는 role claim 불포함 → 재발급 시 DB에서 검증하므로 불필요
*/

package com.capstone.eqh.global.jwt;

import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {
    // 30분
    private static final long ACCESS_TOKEN_VALIDITY_MS = 1000L * 60 * 30;
    // 7일
    private static final long REFRESH_TOKEN_VALIDITY_MS = 1000L * 60 * 30 * 24 * 7;

    private final SecretKey secretKey;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // -- 토큰 생성 ---------------------------------------------------
    public String generateAccessToken(Long userId, String role) {
        return buildToken(String.valueOf(userId), role, ACCESS_TOKEN_VALIDITY_MS);
    }
    public String generateRefreshToken(Long userId) {
        return buildToken(String.valueOf(userId), null, REFRESH_TOKEN_VALIDITY_MS);
    }

    private String buildToken(String subject, String role, long validityMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    // -- 토큰 검증 ------------------------------------------------------
    // 토큰 유효성 검사, 실패 시 CustomException 에 던짐
     public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
     }

     // 만료된 토큰이라도 Claims 추출 가능 (reissue 엔드 포인트 전용)
    public boolean isTokenExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // --Claims 추출 ---------------------------------------------------
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * 서명은 검증하되 만료 여부는 무시하고 userId를 추출한다.
     * reissue 엔드포인트 전용 — DB expiryDate가 만료 판단의 기준이 된다.
     */
    public Long getUserIdIgnoringExpiry(String token) {
        return Long.valueOf(parseClaimsAllowExpired(token).getSubject());
    }

    //--내부 유틸 -------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Claims parseClaimsAllowExpired(String token) {
        try {
            return parseClaims(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
