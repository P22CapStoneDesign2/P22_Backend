package com.capstone.eqh.domain.user.password.entity;

import com.capstone.eqh.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 1회용 토큰.
 * <p>
 * 만료·사용 여부는 DB 컬럼으로 관리하며, 재요청 시 기존 미사용 토큰은
 * {@code expired_at} 을 현재 시각으로 당겨 무효화한다.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken(User user, String token, LocalDateTime expiredAt) {
        this.user = user;
        this.token = token;
        this.expiredAt = expiredAt;
        this.createdAt = LocalDateTime.now();
    }

    /** 비밀번호 변경 성공 후 1회 사용 처리 */
    public void markUsed() {
        this.used = true;
    }

    /** 만료 시각이 지났는지 여부 */
    public boolean isExpired(LocalDateTime now) {
        return expiredAt.isBefore(now) || expiredAt.isEqual(now);
    }

    /** 검증·재설정에 사용 가능한 토큰인지 */
    public boolean isUsable(LocalDateTime now) {
        return !used && !isExpired(now);
    }
}
