package org.cassidylabs.marginalia.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken 도메인")
class RefreshTokenTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("create() — 필드 저장 확인")
    void create_storesFields() {
        Instant expiresAt = Instant.now().plusSeconds(3600);

        RefreshToken token = RefreshToken.create(USER_ID, "token_hash", expiresAt);

        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getToken()).isEqualTo("token_hash");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("isExpired() — 만료 시각 이후면 true")
    void isExpired_returnsTrueWhenPastExpiry() {
        RefreshToken token = RefreshToken.create(USER_ID, "hash", Instant.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isExpired() — 만료 시각 이전이면 false")
    void isExpired_returnsFalseWhenBeforeExpiry() {
        RefreshToken token = RefreshToken.create(USER_ID, "hash", Instant.now().plusSeconds(3600));

        assertThat(token.isExpired()).isFalse();
    }
}
