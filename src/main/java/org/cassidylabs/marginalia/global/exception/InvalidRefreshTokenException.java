package org.cassidylabs.marginalia.global.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super("유효하지 않거나 만료된 Refresh Token입니다.", HttpStatus.UNAUTHORIZED);
    }
}
