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

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

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
    public TokenResult refresh(String refreshToken) {
        RefreshToken token = refreshTokenPort.findByToken(refreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.isExpired()) {
            refreshTokenPort.deleteByToken(refreshToken);
            throw new InvalidRefreshTokenException();
        }

        // Rotation: 기존 폐기 후 새 쌍 발급
        refreshTokenPort.deleteByToken(refreshToken);
        return issueTokenPair(token.getUserId());
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenPort.deleteByToken(refreshToken);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private TokenResult issueTokenPair(UUID userId) {
        // 단일 세션: 기존 refresh token 모두 폐기
        refreshTokenPort.deleteAllByUserId(userId);

        String accessToken = tokenPort.generateAccessToken(userId);

        String rawRefreshToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(refreshExpiryMs);
        refreshTokenPort.save(RefreshToken.create(userId, rawRefreshToken, expiresAt));

        return new TokenResult(accessToken, rawRefreshToken);
    }
}
