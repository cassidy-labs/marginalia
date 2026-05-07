package org.cassidylabs.marginalia.global.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException() {
        super("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
    }
}
