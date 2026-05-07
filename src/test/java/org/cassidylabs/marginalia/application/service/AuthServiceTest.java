package org.cassidylabs.marginalia.application.service;

import org.cassidylabs.marginalia.application.port.in.auth.AuthUseCase.*;
import org.cassidylabs.marginalia.application.port.in.auth.AuthUseCase;
import org.cassidylabs.marginalia.application.port.out.PasswordHasher;
import org.cassidylabs.marginalia.application.port.out.RefreshTokenPort;
import org.cassidylabs.marginalia.application.port.out.TokenPort;
import org.cassidylabs.marginalia.application.port.out.UserPort;
import org.cassidylabs.marginalia.domain.user.RefreshToken;
import org.cassidylabs.marginalia.domain.user.User;
import org.cassidylabs.marginalia.global.exception.EmailAlreadyExistsException;
import org.cassidylabs.marginalia.global.exception.InvalidCredentialsException;
import org.cassidylabs.marginalia.global.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserPort userPort;
    @Mock RefreshTokenPort refreshTokenPort;
    @Mock TokenPort tokenPort;
    @Mock PasswordHasher passwordHasher;

    @InjectMocks AuthService authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@example.com";
    private static final String RAW_PW = "password1234";
    private static final String HASHED_PW = "$2a$bcrypt_hash";
    private static final String ACCESS_TOKEN = "eyJ.access.token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiryMs", 604_800_000L);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() — 신규 이메일이면 토큰 쌍 반환")
    void register_success() {
        given(userPort.existsByEmail(EMAIL)).willReturn(false);
        given(passwordHasher.hash(RAW_PW)).willReturn(HASHED_PW);
        given(userPort.save(any())).willReturn(userWithId());
        given(tokenPort.generateAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);

        TokenResult result = authService.register(new RegisterCommand(EMAIL, RAW_PW));

        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("register() — 중복 이메일이면 EmailAlreadyExistsException")
    void register_duplicateEmail() {
        given(userPort.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterCommand(EMAIL, RAW_PW)))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("register() — DB에 저장되는 refresh token은 raw token의 SHA-256 해시")
    void register_storesHashedRefreshToken() {
        given(userPort.existsByEmail(EMAIL)).willReturn(false);
        given(passwordHasher.hash(RAW_PW)).willReturn(HASHED_PW);
        given(userPort.save(any())).willReturn(userWithId());
        given(tokenPort.generateAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);

        TokenResult result = authService.register(new RegisterCommand(EMAIL, RAW_PW));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenPort).should().save(captor.capture());

        String storedToken = captor.getValue().getToken();
        // SHA-256 hex = 정확히 64자
        assertThat(storedToken).hasSize(64);
        // DB에 저장된 값은 클라이언트에 반환된 raw token과 달라야 함
        assertThat(storedToken).isNotEqualTo(result.refreshToken());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() — 올바른 자격증명이면 토큰 쌍 반환")
    void login_success() {
        given(userPort.findByEmail(EMAIL)).willReturn(Optional.of(userWithId()));
        given(passwordHasher.matches(RAW_PW, HASHED_PW)).willReturn(true);
        given(tokenPort.generateAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);

        TokenResult result = authService.login(new LoginCommand(EMAIL, RAW_PW));

        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("login() — 존재하지 않는 이메일이면 InvalidCredentialsException")
    void login_emailNotFound() {
        given(userPort.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand(EMAIL, RAW_PW)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login() — 비밀번호 불일치면 InvalidCredentialsException")
    void login_wrongPassword() {
        given(userPort.findByEmail(EMAIL)).willReturn(Optional.of(userWithId()));
        given(passwordHasher.matches(RAW_PW, HASHED_PW)).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand(EMAIL, RAW_PW)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login() — 로그인 시 기존 refresh token 전부 폐기(단일 세션)")
    void login_revokesExistingRefreshTokens() {
        given(userPort.findByEmail(EMAIL)).willReturn(Optional.of(userWithId()));
        given(passwordHasher.matches(RAW_PW, HASHED_PW)).willReturn(true);
        given(tokenPort.generateAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);

        authService.login(new LoginCommand(EMAIL, RAW_PW));

        then(refreshTokenPort).should().deleteAllByUserId(USER_ID);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh() — 유효한 토큰이면 새 토큰 쌍 반환 (Rotation)")
    void refresh_success() {
        String rawToken = "validRawToken";
        RefreshToken stored = storedToken(rawToken, Instant.now().plusSeconds(3600));
        given(refreshTokenPort.findByToken(any())).willReturn(Optional.of(stored));
        given(tokenPort.generateAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);

        TokenResult result = authService.refresh(rawToken);

        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isNotBlank();
        // 기존 토큰 삭제 확인
        then(refreshTokenPort).should().deleteByToken(any());
    }

    @Test
    @DisplayName("refresh() — 존재하지 않는 토큰이면 InvalidRefreshTokenException")
    void refresh_tokenNotFound() {
        given(refreshTokenPort.findByToken(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknownToken"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh() — 만료된 토큰이면 삭제 후 InvalidRefreshTokenException")
    void refresh_expiredToken() {
        String rawToken = "expiredToken";
        RefreshToken expired = storedToken(rawToken, Instant.now().minusSeconds(1));
        given(refreshTokenPort.findByToken(any())).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(refreshTokenPort).should().deleteByToken(any());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout() — deleteByToken 호출")
    void logout_deletesToken() {
        authService.logout("someRawToken");

        then(refreshTokenPort).should().deleteByToken(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User userWithId() {
        return User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .passwordHash(HASHED_PW)
                .build();
    }

    private RefreshToken storedToken(String rawToken, Instant expiresAt) {
        // AuthService는 raw token의 hash를 저장하므로, 테스트에서는
        // findByToken(hash) 호출을 any()로 매칭하고 RefreshToken을 직접 반환
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .token(rawToken)   // 테스트용 임의값 (hash 검증은 별도 테스트에서)
                .expiresAt(expiresAt)
                .build();
    }
}
