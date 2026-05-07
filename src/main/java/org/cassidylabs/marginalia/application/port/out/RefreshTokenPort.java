package org.cassidylabs.marginalia.application.port.out;

import org.cassidylabs.marginalia.domain.user.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenPort {

    Optional<RefreshToken> findByToken(String token);

    RefreshToken save(RefreshToken refreshToken);

    /** 단일 세션 보장: 로그인 시 기존 토큰 모두 삭제 */
    void deleteAllByUserId(UUID userId);

    void deleteByToken(String token);
}
