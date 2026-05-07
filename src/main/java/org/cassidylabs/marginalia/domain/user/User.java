package org.cassidylabs.marginalia.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.cassidylabs.marginalia.domain.BaseTimeEntity;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // ── 팩토리 메서드 ──────────────────────────────────────────────────────────

    public static User create(String email, String passwordHash) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .build();
    }

    // ── 도메인 메서드 ──────────────────────────────────────────────────────────

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
