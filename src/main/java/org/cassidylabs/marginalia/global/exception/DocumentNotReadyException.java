package org.cassidylabs.marginalia.global.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DocumentNotReadyException extends BusinessException {

    public DocumentNotReadyException(UUID id) {
        super("업로드가 완료되지 않은 문서입니다: " + id, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
