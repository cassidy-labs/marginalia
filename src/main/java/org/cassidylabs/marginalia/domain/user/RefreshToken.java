package org.cassidylabs.marginalia.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.cassidylabs.marginalia.domain.BaseTimeEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * SHA-256(rawToken) hex digest — 64자 고정.
     * Raw token은 발급 시 클라이언트에만 전달되고 DB에는 절대 저장되지 않는다.
     * DB 읽기 누출이 발생해도 원본 토큰을 복원할 수 없다.
     */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // ── 팩토리 메서드 ──────────────────────────────────────────────────────────

    public static RefreshToken create(UUID userId, String token, Instant expiresAt) {
        return RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    // ── 도메인 메서드 ──────────────────────────────────────────────────────────

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
