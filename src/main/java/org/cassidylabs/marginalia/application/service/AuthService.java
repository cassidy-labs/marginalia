package org.cassidylabs.marginalia.application.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserPort userPort;
    private final RefreshTokenPort refreshTokenPort;
    private final TokenPort tokenPort;
    private final PasswordHasher passwordHasher;

    @Value("${jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    @Override
    public TokenResult register(RegisterCommand command) {
        if (userPort.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException();
        }
        User user = userPort.save(User.create(command.email(), passwordHasher.hash(command.password())));
        return issueTokenPair(user.getId());
    }

    @Override
    public TokenResult login(LoginCommand command) {
        User user = userPort.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokenPair(user.getId());
    }

    @Override
    public TokenResult refresh(String rawToken) {
        String tokenHash = hashToken(rawToken);
        RefreshToken token = refreshTokenPort.findByToken(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.isExpired()) {
            refreshTokenPort.deleteByToken(tokenHash);
            throw new InvalidRefreshTokenException();
        }

        // Rotation: 기존 폐기 후 새 쌍 발급
        refreshTokenPort.deleteByToken(tokenHash);
        return issueTokenPair(token.getUserId());
    }

    @Override
    public void logout(String rawToken) {
        refreshTokenPort.deleteByToken(hashToken(rawToken));
    }

    // ── private ───────────────────────────────────────────────────────────────

    private TokenResult issueTokenPair(UUID userId) {
        // 단일 세션: 기존 refresh token 모두 폐기
        refreshTokenPort.deleteAllByUserId(userId);

        String accessToken = tokenPort.generateAccessToken(userId);

        String rawToken = generateRawToken();
        Instant expiresAt = Instant.now().plusMillis(refreshExpiryMs);
        refreshTokenPort.save(RefreshToken.create(userId, hashToken(rawToken), expiresAt));

        return new TokenResult(accessToken, rawToken);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
