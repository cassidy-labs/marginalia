package org.cassidylabs.marginalia.application.port.in.auth;

public interface AuthUseCase {

    record RegisterCommand(String email, String password) {}

    record LoginCommand(String email, String password) {}

    /** accessToken: 응답 바디, refreshToken: httpOnly 쿠키에 세팅 */
    record TokenResult(String accessToken, String refreshToken) {}

    TokenResult register(RegisterCommand command);

    TokenResult login(LoginCommand command);

    /** Refresh Token Rotation: 기존 토큰 폐기 + 새 토큰 쌍 반환 */
    TokenResult refresh(String refreshToken);

    void logout(String refreshToken);
}
