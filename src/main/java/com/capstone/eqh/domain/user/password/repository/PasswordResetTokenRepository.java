package com.capstone.eqh.domain.user.password.repository;

import com.capstone.eqh.domain.user.password.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * 사용자의 미사용·미만료 토큰 목록 (재요청 시 일괄 무효화용).
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.user.id = :userId
              AND t.used = false
              AND t.expiredAt > :now
            """)
    List<PasswordResetToken> findActiveTokensByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    /**
     * DB-only 쿨다운 대안: 가장 최근 토큰 생성 시각 조회.
     */
    Optional<PasswordResetToken> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * 기존 활성 토큰을 즉시 만료 처리 (expired_at = now).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t
            SET t.expiredAt = :now
            WHERE t.user.id = :userId
              AND t.used = false
              AND t.expiredAt > :now
            """)
    int expireActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
