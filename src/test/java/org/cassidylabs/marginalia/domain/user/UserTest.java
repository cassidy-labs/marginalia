package org.cassidylabs.marginalia.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 도메인")
class UserTest {

    @Test
    @DisplayName("create() — 이메일·해시 저장, ID는 null(JPA 발급 전)")
    void create_storesEmailAndHash() {
        User user = User.create("test@example.com", "hashed_pw");

        assertThat(user.getId()).isNull();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed_pw");
    }

    @Test
    @DisplayName("changePassword() — 비밀번호 해시 교체")
    void changePassword_updatesHash() {
        User user = User.create("test@example.com", "old_hash");

        user.changePassword("new_hash");

        assertThat(user.getPasswordHash()).isEqualTo("new_hash");
    }
}
