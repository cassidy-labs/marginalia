package org.cassidylabs.marginalia.application.port.out;

import java.util.UUID;

/** JWT 생성·검증 포트. 구현체: infrastructure/security/JwtTokenProvider */
public interface TokenPort {

    String generateAccessToken(UUID userId);

    UUID extractUserId(String token);

    boolean isValid(String token);
}
